package it.davidlab.timeslot.service;

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
import it.davidlab.timeslot.domain.AssetModel;
import it.davidlab.timeslot.domain.AssetType;
import it.davidlab.timeslot.domain.TimeslotProps;
import it.davidlab.timeslot.dto.AssetInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AdminController {


    private AlgoService algoService;

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    public AdminController(AlgoService algoService) {
        this.algoService = algoService;
    }


    @GetMapping(path = "/ticket/available")
    @ResponseBody
    public List<AssetInfo> timeslotList(Principal principal) throws Exception {

        com.algorand.algosdk.account.Account adminAccount = algoService.getAccount(principal.getName());

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(adminAccount.getAddress()).execute().body();

        List<AssetHolding> assets = account.assets;

        List<AssetInfo> assetInfo = new ArrayList<>();

        assets.stream().filter(a -> !StringUtils.isEmpty(a.creator)).forEach(a -> {
            Optional<AssetInfo> asset = algoService.getAssetProperties(a.assetId, a.amount.longValue());
            asset.ifPresent(as -> {
                as.setAmount(a.amount.longValue());
                assetInfo.add(as);
            });
        });

        return assetInfo;
    }


    @GetMapping(path = "/ticket/archived")
    @ResponseBody
    public List<AssetInfo> archivedTicket(Principal principal) throws Exception {

        com.algorand.algosdk.account.Account adminAccount = algoService.getArchiveAccount();

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(adminAccount.getAddress()).execute().body();

        List<AssetHolding> assets = account.assets;

        List<AssetInfo> assetInfo = new ArrayList<>();

        assets.stream().filter(a -> !StringUtils.isEmpty(a.creator)).forEach(a -> {
            Optional<AssetInfo> asset = algoService.getAssetProperties(a.assetId, a.amount.longValue());
            asset.ifPresent(as -> {
                as.setAmount(a.amount.longValue());
                assetInfo.add(as);
            });
        });

        return assetInfo;
    }


    /**
     * Create two new tokens for a new timeslot
     *
     * @param assetModel
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/timeslot/create", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public String createTsPool(@RequestBody AssetModel assetModel, Principal principal) throws Exception {

        com.algorand.algosdk.account.Account adminAccount = algoService.getAdminAccount();

        boolean defaultFrozen = false;
        String unitName = assetModel.getUnitName();
        String assetName = assetModel.getAssetName();
        long assetTotal = assetModel.getAssetTotal();
        int assettDecimals = assetModel.getAssetDecimals();
        String url = assetModel.getUrl();

        //TODO is it necessary to set all the addresses?
        Address manager = adminAccount.getAddress();
        Address reserve = adminAccount.getAddress();;
        Address freeze = adminAccount.getAddress();;
        Address clawback = adminAccount.getAddress();;

        //TODO
        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();

        long startTimestamp = assetModel.getAssetParams().getStartValidity();
        long endTimestamp = assetModel.getAssetParams().getEndValidity();
        String description = assetModel.getAssetParams().getDescription();
        long price = assetModel.getAssetParams().getPrice();

        // Ticket Asset
        TimeslotProps tsParams = new TimeslotProps(startTimestamp, endTimestamp, -1, TimeUnit.HOURS,
                description, price, AssetType.TICKET);


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
            assetIndex = algoService.getClient().PendingTransactionInformation(txId).execute().body().assetIndex;

        } else {
            throw new IllegalStateException();
        }

        return "{\"assetIndex\":" + assetIndex + ", \"txId\":\"" + txId + "\"}";
    }




    @PostMapping(value = "/timeslot/receipt/create", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public String createTsRcp(@RequestBody AssetModel assetModel, Principal principal) throws Exception {

        com.algorand.algosdk.account.Account adminAccount = algoService.getAccount(principal.getName());

        boolean defaultFrozen = false;
        String unitName = assetModel.getUnitName();
        String assetName = assetModel.getAssetName();
        long assetTotal = assetModel.getAssetTotal();
        int assettDecimals = assetModel.getAssetDecimals();
        String url = assetModel.getUrl();

        Address manager = adminAccount.getAddress();
        Address reserve = adminAccount.getAddress();;
        Address freeze = adminAccount.getAddress();;
        Address clawback = adminAccount.getAddress();;

        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();

        long startTimestamp = assetModel.getAssetParams().getStartValidity();
        long endTimestamp = assetModel.getAssetParams().getEndValidity();
        String description = assetModel.getAssetParams().getDescription();
        long price = assetModel.getAssetParams().getPrice();

        // Ticket Asset
        TimeslotProps assetTParams = new TimeslotProps(startTimestamp, endTimestamp, -1, TimeUnit.HOURS,
                description, price, AssetType.TICKET);
        byte[] encAssetTProps = Encoder.encodeToMsgPack(assetTParams);
        byte[] propsHashT = Encoder.encodeToBase64(DigestUtils.md5(encAssetTProps)).getBytes(StandardCharsets.UTF_8);
        String assetTName = "#" + assetName;
        String unitTName = "<#>" + unitName;

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
        TimeslotProps assetRParams = new TimeslotProps(startTimestamp, endTimestamp, -1, TimeUnit.HOURS,
                description, price, AssetType.RECEIPT);
        byte[] encAssetRProps = Encoder.encodeToMsgPack(assetRParams);
        byte[] propsHashR = Encoder.encodeToBase64(DigestUtils.md5(encAssetRProps)).getBytes(StandardCharsets.UTF_8);
        String assetRName = assetName;
        String unitRName = "<>" + unitName;

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
                .note(encAssetRProps)
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
            assetIndex = algoService.getClient().PendingTransactionInformation(txId).execute().body().assetIndex;

        } else {
            throw new IllegalStateException();
        }

        return "{\"assetIndex\":" + assetIndex + ", \"txId\":\"" + txId + "\"}";
    }


}
