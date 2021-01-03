package it.davidlab.timeslot.service;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.transaction.TxGroup;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.PostTransactionsResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;
import it.davidlab.timeslot.domain.AssetModel;
import it.davidlab.timeslot.domain.AssetType;
import it.davidlab.timeslot.domain.TimeslotProps;
import it.davidlab.timeslot.entity.AccountEntity;
import it.davidlab.timeslot.repository.AccountRepo;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/timeslot/admin")
public class AdminController {


    private AlgoService algoService;
    private AccountRepo accountRepo;

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    public AdminController(AccountRepo accountRepo, AlgoService algoService) {
        this.accountRepo = accountRepo;
        this.algoService = algoService;
    }

//    @PostConstruct
//    public void init() {
//        client = new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
//        indexerClient = new IndexerClient(INDEXER_API_ADDR, INDEXER_API_PORT);
//    }



    /**
     * Create two new tokens for a new timeslot
     *
     * @param assetModel
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/create", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public String createAssetPool(@RequestBody AssetModel assetModel, Principal principal) throws Exception {

        AccountEntity currentAccount = accountRepo.getByUsername(principal.getName());

        Address adminAddress = new Address(currentAccount.getAddress());

        com.algorand.algosdk.account.Account adminAccount =
                new com.algorand.algosdk.account.Account(currentAccount.getPassphrase());

        boolean defaultFrozen = false;
        String unitName = assetModel.getUnitName();
        String assetName = assetModel.getAssetName();
        long assetTotal = assetModel.getAssetTotal();
        int assettDecimals = assetModel.getAssetDecimals();
        String url = assetModel.getUrl();

        Address manager = adminAddress;
        Address reserve = adminAddress;
        Address freeze = adminAddress;
        Address clawback = adminAddress;

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
                .sender(adminAddress)
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
                .sender(adminAddress)
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
