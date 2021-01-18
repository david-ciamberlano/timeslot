package it.davidlab.timeslot.controller;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.transaction.TxGroup;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.AssetHolding;
import com.algorand.algosdk.v2.client.model.PostTransactionsResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.davidlab.timeslot.domain.*;
import it.davidlab.timeslot.service.AlgoService;
import it.davidlab.timeslot.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/admin")
public class AdminAPI {


    private AlgoService algoService;
    private UserService userService;


    private static final Logger logger = LoggerFactory.getLogger(AdminAPI.class);

    public AdminAPI(AlgoService algoService, UserService userService) {
        this.userService = userService;
        this.algoService = algoService;
    }

    @Operation(summary = "Create a new User")
    @PostMapping(path = "/v1/user", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void createUser(@RequestBody UserModel user) {
        userService.createUser(user);
    }


    @Operation(summary = "Get available/archived timeslot.")
    @GetMapping(path = "/v1/timeslots")
    @ResponseBody
    public List<AssetInfo> timeslotList(Principal principal,
                                        @Parameter(description = "available | archived")
                                        @RequestParam String filter) throws Exception {

        Account currentAccount;
        if (filter.equals("available")) {
            currentAccount = algoService.getAccount(principal.getName());
        } else if (filter.equals("archived")) {
            currentAccount = algoService.getArchiveAccount();
        } else {
            throw new Exception("Error");
        }

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(currentAccount.getAddress()).execute().body();

        List<AssetHolding> assets = account.assets;

        List<AssetInfo> assetInfo = new ArrayList<>();

        //TODO Filter the expired
        assets.stream().filter(a -> !StringUtils.isEmpty(a.creator)).forEach(a -> {
            Optional<AssetInfo> asset = algoService.getAssetProperties(a.assetId, a.amount.longValue());
            asset.ifPresent(as -> {
                as.setAmount(a.amount.longValue());
                assetInfo.add(as);
            });
        });

        return assetInfo;
    }


    @PostMapping(value = "/v1/timeslots/{id}/send/{amount}/to/{username}")
    public void sendTimeslot(Principal principal, @PathVariable long id,
                             @PathVariable long amount, @PathVariable String username) throws Exception {

        Account adminAccount = algoService.getAccount(principal.getName());
        Account receiverAccount = algoService.getAccount(username);

        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();

        com.algorand.algosdk.transaction.Transaction purchaseTx =
                com.algorand.algosdk.transaction.Transaction
                .AssetTransferTransactionBuilder()
                .sender(adminAccount.getAddress())
                .assetReceiver(receiverAccount.getAddress())
                .assetIndex(id)
                .suggestedParams(params)
//                .note("encodedTxMsg")
                .assetAmount(amount)
                .build();

        algoService.checkOptIn(id, receiverAccount);

        algoService.sendTransaction(adminAccount, purchaseTx);

    }



    /**
     * Create new ticket
     *
     * @param assetModel
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/v1/timeslots", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void createTimeslots(@RequestBody AssetModel assetModel, Principal principal) throws Exception {

        Account adminAccount = algoService.getAccount(principal.getName());

        boolean defaultFrozen = assetModel.isDefaultFrozen();
        String unitName = assetModel.getUnitName();
        String assetName = assetModel.getAssetName();
        long assetTotal = assetModel.getAssetTotal();
        int assettDecimals = assetModel.getAssetDecimals();
        String url = assetModel.getUrl();

        //TODO is it necessary to set all the addresses?
        Address manager = adminAccount.getAddress();
        Address reserve = adminAccount.getAddress();
        ;
        Address freeze = adminAccount.getAddress();
        ;
        Address clawback = adminAccount.getAddress();
        ;

        //TODO
        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();

        TimeslotProps timeslotProps = assetModel.getTimeslotProperties();

        long startTimestamp = timeslotProps.getStartValidity();
        long endTimestamp = timeslotProps.getEndValidity();
        String description = timeslotProps.getDescription();
        long price = timeslotProps.getPrice();
        long duration = timeslotProps.getDuration();
        TsLocation tsLocation = timeslotProps.getTsLocation();

        // Ticket Asset
        TimeslotProps tsParams = new TimeslotProps(startTimestamp, endTimestamp, duration, TimeUnit.HOURS,
                price, tsLocation, AssetType.TICKET, description);


        byte[] encAssetProps = Encoder.encodeToMsgPack(tsParams);
        String assetRName = assetName;
        String unitRName = unitName;

        Transaction txTicket = Transaction.AssetCreateTransactionBuilder()
                .sender(adminAccount.getAddress())
                .assetTotal(assetTotal)
                .assetDecimals(assettDecimals)
                .assetUnitName(unitRName)
                .assetName(assetRName)
                .url(url)
                .manager(manager)
                .reserve(reserve)
                .freeze(freeze)
                .defaultFrozen(defaultFrozen)
                .clawback(clawback)
                .note(encAssetProps)
//???                .metadataHash(propsHashR)
                .suggestedParams(params)
                .build();

        // Set the tx Fees
        BigInteger origfee = BigInteger.valueOf(params.fee);
        Account.setFeeByFeePerByte(txTicket, origfee);

        SignedTransaction signedTx = adminAccount.signTransaction(txTicket);
        byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTx);

        Response<PostTransactionsResponse> txResponse =
                algoService.getClient().RawTransaction().rawtxn(encodedTxBytes).execute();

        Long assetIndex;
        String txId;

        if (txResponse.isSuccessful()) {
            txId = txResponse.body().txId;
            logger.info("Transaction id: ", txId);
            // write transaction to node
            algoService.waitForConfirmation(txId, 6);
//            assetIndex = algoService.getClient().PendingTransactionInformation(txId).execute().body().assetIndex;

        } else {
            throw new IllegalStateException();
        }

    }


    @PostMapping(value = "/v1/ticketpair", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void createTimeslotReceipts(@RequestBody AssetModel assetModel, Principal principal) throws Exception {

        Account adminAccount = algoService.getAdminAccount();

        boolean defaultFrozen = false;
        String unitName = assetModel.getUnitName();
        String assetName = assetModel.getAssetName();
        long assetTotal = assetModel.getAssetTotal();
        int assettDecimals = assetModel.getAssetDecimals();
        String url = assetModel.getUrl();

        Address manager = adminAccount.getAddress();
        Address reserve = adminAccount.getAddress();
        ;
        Address freeze = adminAccount.getAddress();
        ;
        Address clawback = adminAccount.getAddress();
        ;

        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();
        TimeslotProps timeslotProps = assetModel.getTimeslotProperties();

        long startTimestamp = timeslotProps.getStartValidity();
        long endTimestamp = timeslotProps.getEndValidity();
        String description = timeslotProps.getDescription();
        long price = timeslotProps.getPrice();
        long duration = timeslotProps.getDuration();
        TsLocation tsLocation = timeslotProps.getTsLocation();

        // Ticket Asset
        TimeslotProps timeslotParams = new TimeslotProps(startTimestamp, endTimestamp, duration, TimeUnit.HOURS,
                price, tsLocation, AssetType.TICKET, description);

        byte[] encAssetTProps = Encoder.encodeToMsgPack(timeslotParams);
        byte[] propsHashT = Encoder.encodeToBase64(DigestUtils.md5(encAssetTProps)).getBytes(StandardCharsets.UTF_8);
        String assetTName = "#" + assetName;
        String unitTName = "#" + unitName;

        Transaction txT = Transaction.AssetCreateTransactionBuilder()
                .sender(adminAccount.getAddress())
                .assetTotal(assetTotal)
                .assetDecimals(assettDecimals)
                .assetUnitName(unitTName)
                .assetName(assetTName)
                .url(url)
                .manager(manager)
                .reserve(reserve)
                .freeze(freeze)
                .defaultFrozen(defaultFrozen)
                .clawback(clawback)
                .note(encAssetTProps)
//???               .metadataHash(propsHashT)
                .suggestedParams(params)
                .build();

        // Receipt
        TimeslotProps timeslotRecParams = new TimeslotProps(startTimestamp, endTimestamp, duration, TimeUnit.HOURS,
                price, tsLocation, AssetType.RECEIPT, description);
        byte[] encAssetRecProps = Encoder.encodeToMsgPack(timeslotRecParams);
//???    byte[] propsHashR = Encoder.encodeToBase64(DigestUtils.md5(encAssetRecProps)).getBytes(StandardCharsets.UTF_8);
        String assetRName = assetName;
        String unitRName = unitName;

        Transaction txR = Transaction.AssetCreateTransactionBuilder()
                .sender(adminAccount.getAddress())
                .assetTotal(assetTotal)
                .assetDecimals(assettDecimals)
                .assetUnitName(unitRName)
                .assetName(assetRName)
                .url(url)
                .manager(manager)
                .reserve(reserve)
                .freeze(freeze)
                .defaultFrozen(defaultFrozen)
                .clawback(clawback)
                .note(encAssetRecProps)
//???                .metadataHash(propsHashR)
                .suggestedParams(params)
                .build();

        // Set the tx Fees
        BigInteger origfee = BigInteger.valueOf(params.fee);
        Account.setFeeByFeePerByte(txT, origfee);
        Account.setFeeByFeePerByte(txR, origfee);

        // Build the Group
        Digest gid = TxGroup.computeGroupID(txT, txR);
        txT.assignGroupID(gid);
        txR.assignGroupID(gid);

        SignedTransaction signedTxT = adminAccount.signTransaction(txT);
        SignedTransaction signedTxR = adminAccount.signTransaction(txR);

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        byte[] encodedTxBytesT = Encoder.encodeToMsgPack(signedTxT);
        byte[] encodedTxBytesR = Encoder.encodeToMsgPack(signedTxR);
        byteOutputStream.write(encodedTxBytesT);
        byteOutputStream.write(encodedTxBytesR);
        byte[] groupTransactBytes = byteOutputStream.toByteArray();

        Response<PostTransactionsResponse> txResponse =
                algoService.getClient().RawTransaction().rawtxn(groupTransactBytes).execute();

        Long assetIndex;
        String txId;

        if (txResponse.isSuccessful()) {
            txId = txResponse.body().txId;
            logger.info("Transaction id: ", txId);
            // write transaction to node
            algoService.waitForConfirmation(txId, 6);
//            assetIndex = algoService.getClient().PendingTransactionInformation(txId).execute().body().assetIndex;

        } else {
            throw new IllegalStateException();
        }

    }


}
