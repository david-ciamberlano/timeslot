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
import io.swagger.annotations.Api;
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
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Api(value = "Administration API")
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


    @Operation(summary = "Get available/archived timeslots")
    @GetMapping(path = "/v1/timeslots")
    @ResponseBody
    public List<TimeslotDto> timeslotList(Principal principal, @Parameter(description = "available | archived")
                                          @RequestParam String f) throws Exception {

        Account currentAccount;
        switch (f) {
            case "available":
                currentAccount = algoService.getAccount(principal.getName());
                break;
            case "archived":
                currentAccount = algoService.getArchiveAccount();
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong Filter");
        }

        List<AssetHolding> assets = algoService.getAccountAssets(currentAccount.getAddress());

        List<TimeslotDto> timeslotDtos =

        //TODO Filter the expired Timeslots
        assets.stream().filter(a -> !StringUtils.isBlank(a.creator))
                .map(a ->  algoService.getAssetProperties(a.assetId, a.amount.longValue()))
                .filter(Optional::isPresent).map(Optional::get)
                .collect (Collectors.toList());

        return timeslotDtos;
    }


    @Operation(summary = "send timeslots to a username")
    @PostMapping(value = "/v1/timeslots/{id}/send/{amount}/to/{username}")
    public void sendTimeslot(Principal principal, @PathVariable long id,
                             @RequestBody TimeslotTransferDto timeslotTransfer) throws Exception {

        String username = timeslotTransfer.getUsername();
        long amount = timeslotTransfer.getAmount();
        String note = timeslotTransfer.getTransferNote();

        Account adminAccount = algoService.getAccount(principal.getName());
        Account receiverAccount = algoService.getAccount(username);

        TransactionParametersResponse params = algoService.getTxParams();

        com.algorand.algosdk.transaction.Transaction purchaseTx =
                Transaction
                        .AssetTransferTransactionBuilder()
                        .sender(adminAccount.getAddress())
                        .assetReceiver(receiverAccount.getAddress())
                        .assetIndex(id)
                        .suggestedParams(params)
                        .note(note.getBytes(StandardCharsets.UTF_8))
                        .assetAmount(amount)
                        .build();

        algoService.checkOptIn(id, receiverAccount);
        algoService.sendTransaction(adminAccount, purchaseTx);

    }


    /**
     * Create new timeslots
     *
     * @param timeslotModel
     * @return
     * @throws Exception
     */
    @Operation(summary = "Create timeslots")
    @PostMapping(value = "/v1/timeslots", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void createTimeslots(@RequestBody TimeslotModel timeslotModel, Principal principal) throws Exception {

        Account adminAccount = algoService.getAccount(principal.getName());

        boolean defaultFrozen = timeslotModel.isDefaultFrozen();
        String unitName = timeslotModel.getUnitName();
        String assetName = timeslotModel.getAssetName();
        long assetTotal = timeslotModel.getAssetTotal();
        int assettDecimals = timeslotModel.getAssetDecimals();
        String url = timeslotModel.getUrl();

        //TODO is it necessary to set all the addresses?
        Address manager = adminAccount.getAddress();
        Address reserve = adminAccount.getAddress();
        Address freeze = adminAccount.getAddress();
        Address clawback = adminAccount.getAddress();

        TransactionParametersResponse params = algoService.getTxParams();

        TimeslotProperties timeslotProperties = timeslotModel.getTimeslotProperties();

        long startTimestamp = timeslotProperties.getStartValidity();
        long endTimestamp = timeslotProperties.getEndValidity();
        String description = timeslotProperties.getDescription();
        long price = timeslotProperties.getPrice();
        long duration = timeslotProperties.getDuration();
        TimeslotLocation timeslotLocation = timeslotProperties.getTimeslotLocation();
        TimeslotUnit timeslotUnit = timeslotProperties.getTimeslotUnit();

        // Ticket Asset
        TimeslotProperties tsParams = new TimeslotProperties(startTimestamp, endTimestamp, duration, timeslotUnit,
                price, timeslotLocation, TimeslotType.TICKET, description);

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
            algoService.waitForConfirmation(txId, 6);

        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction not accepted");
        }

    }

    @Operation(summary = "Create new timeslotpairs")
    @PostMapping(value = "/v1/timeslotpairs", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void createTimeslotPairs(@RequestBody TimeslotModel timeslotModel) throws Exception {

        Account adminAccount = algoService.getAdminAccount();

        boolean defaultFrozen = false;
        String unitName = timeslotModel.getUnitName();
        String assetName = timeslotModel.getAssetName();
        long assetTotal = timeslotModel.getAssetTotal();
        int assettDecimals = timeslotModel.getAssetDecimals();
        String url = timeslotModel.getUrl();

        Address manager = adminAccount.getAddress();
        Address reserve = adminAccount.getAddress();
        Address freeze = adminAccount.getAddress();
        Address clawback = adminAccount.getAddress();

        TransactionParametersResponse params = algoService.getTxParams();
        TimeslotProperties timeslotProperties = timeslotModel.getTimeslotProperties();

        long startTimestamp = timeslotProperties.getStartValidity();
        long endTimestamp = timeslotProperties.getEndValidity();
        String description = timeslotProperties.getDescription();
        long price = timeslotProperties.getPrice();
        long duration = timeslotProperties.getDuration();
        TimeslotLocation timeslotLocation = timeslotProperties.getTimeslotLocation();
        TimeslotUnit timeslotUnit = timeslotProperties.getTimeslotUnit();

        // Ticket Asset
        TimeslotProperties timeslotParams = new TimeslotProperties(startTimestamp, endTimestamp, duration,
                timeslotUnit, price, timeslotLocation, TimeslotType.PAIR, description);

        byte[] encAssetTProps = Encoder.encodeToMsgPack(timeslotParams);
        byte[] propsHashT = Encoder.encodeToBase64(DigestUtils.md5(encAssetTProps)).getBytes(StandardCharsets.UTF_8);
        String assetTName = "_" + assetName;
        String unitTName = "_" + unitName;

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
        TimeslotProperties timeslotRecParams = new TimeslotProperties(startTimestamp, endTimestamp, duration,
                TimeslotUnit.HOURS, price, timeslotLocation, TimeslotType.RECEIPT, description);
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
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction not accepted");
        }

    }


}
