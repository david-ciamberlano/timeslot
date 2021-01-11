package it.davidlab.timeslot.service;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.TxGroup;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.*;
import io.swagger.annotations.Api;
import it.davidlab.timeslot.domain.AssetType;
import it.davidlab.timeslot.domain.TimeslotProps;
import it.davidlab.timeslot.dto.AssetInfo;
import it.davidlab.timeslot.dto.TxInfo;
import it.davidlab.timeslot.entity.AccountEntity;
import it.davidlab.timeslot.repository.AccountRepo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Api(value="User API")
@RestController
public class UserController {

    private AlgoService algoService;

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(AlgoService algoService) {
        this.algoService = algoService;
    }


    @GetMapping(path = "ticket/avaiable", produces="application/json")
    @ResponseBody
    public List<AssetInfo> ticketAvailable() throws Exception {

        com.algorand.algosdk.account.Account account = algoService.getAccount("admin");

//        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account2 =
                algoService.getClient().AccountInformation(account.getAddress()).execute().body();

        List<AssetHolding> assets = account2.assets;

        List<AssetInfo> assetInfo = assets.stream().filter(a -> !StringUtils.isEmpty(a.creator))
                .map(a-> algoService.getAssetProperties(a.assetId, a.amount.longValue()))
                .filter(a -> a.isPresent() && a.get().getType() == AssetType.TICKET && a.get().getAmount()>0)
                .map(Optional::get).collect(Collectors.toList());

        return assetInfo;
    }


    @GetMapping(path = "/ticket/owned", produces="application/json")
    @ResponseBody
    public List<AssetInfo> ticketOwned(Principal principal) throws Exception {

        com.algorand.algosdk.account.Account currentAccount = algoService.getAccount(principal.getName());

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(currentAccount.getAddress()).execute().body();

        List<AssetHolding> assets = account.assets;

        List<AssetInfo> assetsInfo = assets.stream().filter(a -> !StringUtils.isEmpty(a.creator))
                .map(a-> algoService.getAssetProperties(a.assetId, a.amount.longValue()))
                .filter(a -> a.isPresent() && (a.get().getType() == AssetType.TICKET && a.get().getAmount() > 0))
                .map(Optional::get).collect(Collectors.toList());

        return assetsInfo;
    }




    @GetMapping(path = "/receipt/owned", produces="application/json")
    @ResponseBody
    public List<AssetInfo> receiptOwned(Principal principal) throws Exception {

        com.algorand.algosdk.account.Account currentAccount = algoService.getAccount(principal.getName());

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(currentAccount.getAddress()).execute().body();

        List<AssetHolding> assets = account.assets;

        List<AssetInfo> assetsInfo = assets.stream().filter(a -> !StringUtils.isEmpty(a.creator))
                .map(a-> algoService.getAssetProperties(a.assetId, a.amount.longValue()))
                .filter(a -> a.isPresent() && (a.get().getType() == AssetType.RECEIPT && a.get().getAmount() > 0))
                .map(Optional::get).collect(Collectors.toList());

        return assetsInfo;
    }



    @GetMapping(path = "/transactions/{ticketId}", produces="application/json")
    @ResponseBody
    public List<TxInfo> getTransactions(@PathVariable long ticketId, Principal principal) throws Exception {

        com.algorand.algosdk.account.Account currentAccount = algoService.getAccount(principal.getName());

        List<com.algorand.algosdk.v2.client.model.Transaction> txs = algoService.getIndexerClient()
                .lookupAccountTransactions(currentAccount.getAddress()).txType(Enums.TxType.AXFER)
                .currencyGreaterThan(0L).assetId(ticketId).execute().body().transactions;


        List<TxInfo> txInfos = txs.stream()
                .map(t -> algoService.getTxParams(t, ticketId)).filter(t -> t.getAmount()>0)
                .collect(Collectors.toList());

        return txInfos;
    }


    @GetMapping(path = "/ticket/{ticketid}/details", produces="application/json")
    @ResponseBody
    public AssetInfo ticketDetails(@PathVariable long ticketid, Principal principal) throws Exception {

        AssetInfo assetInfo = algoService.getAssetProperties(ticketid).orElseThrow();

        return assetInfo;
    }


    /**
     * User can buy one or more timeslots
     * @param timeslotIndex
     * @param amount
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/timeslot/{timeslotIndex}/obtain/amount/{amount}", produces="application/json")
    @ResponseBody
    public String getTicket(@PathVariable Long timeslotIndex, @PathVariable int amount,
                              Principal principal) throws Exception {

        Optional<TimeslotProps> timeslotProps = algoService.getAssetParams(timeslotIndex);

        //TODO write custom exception for that
        if (timeslotProps.isEmpty()) {
            throw new IllegalStateException("No properties found for the timeslot");
        }

        long assetPrice = timeslotProps.get().getPrice();

        // get the current user (that is the buyer user)
        com.algorand.algosdk.account.Account buyerAccount = algoService.getAccount(principal.getName());

        //TODO make "admin" parametric
        com.algorand.algosdk.account.Account adminAccount = algoService.getAccount("admin");

        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();

        algoService.checkOptIn(timeslotIndex, buyerAccount);

        long totalPrice = (assetPrice * 1000000L) * (long) amount; //converted in microAlgorand

        com.algorand.algosdk.transaction.Transaction purchaseTx = com.algorand.algosdk.transaction.Transaction.PaymentTransactionBuilder()
                .sender(buyerAccount.getAddress())
                .amount(totalPrice)
                .receiver(adminAccount.getAddress())
                .suggestedParams(params)
                .build();


        //TODO what message?
        byte[] encodedTxMsg = Encoder.encodeToMsgPack("");
        com.algorand.algosdk.transaction.Transaction assetTTx = com.algorand.algosdk.transaction.Transaction
                .AssetTransferTransactionBuilder()
                .sender(adminAccount.getAddress())
                .assetReceiver(buyerAccount.getAddress())
                .assetIndex(timeslotIndex)
                .suggestedParams(params)
                .note(encodedTxMsg)
                .assetAmount(amount)
                .build();


        // Group the transactions
        Digest gid = TxGroup.computeGroupID(purchaseTx, assetTTx);
        purchaseTx.assignGroupID(gid);
        assetTTx.assignGroupID(gid);

        SignedTransaction signedPurchaseTx = buyerAccount.signTransaction(purchaseTx);
        SignedTransaction signedAssetTTx = adminAccount.signTransaction(assetTTx);

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        byte[] encodedTxBytes1 = Encoder.encodeToMsgPack(signedPurchaseTx);
        byte[] encodedTxBytes2 = Encoder.encodeToMsgPack(signedAssetTTx);
        byteOutputStream.write(encodedTxBytes1);
        byteOutputStream.write(encodedTxBytes2);
        byte[] groupTransactionBytes = byteOutputStream.toByteArray();

        Response<PostTransactionsResponse> txResponse =
                algoService.getClient().RawTransaction().rawtxn(groupTransactionBytes).execute();

        if (txResponse.isSuccessful()) {
            String id = txResponse.body().txId;
            logger.info("Transaction id: ", id);
            // write transaction to node
            algoService.waitForConfirmation(id, 6);

        } else {
            //TODO manage Error!!
            return "{\"msg\":\"Error\"}";
        }

        //TODO return a real json object!
        return "{\"msg\":\"OK\"}";
    }




    @PostMapping(value = "/rticket/{timeslotIndex}/validate/amount/{amount}", produces="application/json")
    @ResponseBody
    public String validateTicket(@PathVariable long timeslotIndex, @PathVariable long amount,
                                  Principal principal) throws Exception{

        com.algorand.algosdk.account.Account comsumerAccount = algoService.getAccount(principal.getName());
        com.algorand.algosdk.account.Account adminAccount = algoService.getAdminAccount();
        com.algorand.algosdk.account.Account archiveAccount = algoService.getArchiveAccount();

        algoService.checkOptIn(timeslotIndex, archiveAccount);

        //TODO find a smarter way to retrieve timeslotIndex+1
        long timeslotReceiptIndex = timeslotIndex + 1;
        algoService.checkOptIn(timeslotReceiptIndex, comsumerAccount);

        //TODO check execute first
        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();


//        TxNoteMessage txNoteMessage = new TxNoteMessage(
//                "You miss 100% of the shots you don’t take",
//                "Wayne Gretzky");
        byte[] encodedTxMsg = Encoder.encodeToMsgPack("");

        // consumer consume timeslots
        com.algorand.algosdk.transaction.Transaction ticketValidateTx = com.algorand.algosdk.transaction.Transaction
                .AssetTransferTransactionBuilder()
                .sender(comsumerAccount.getAddress())
                .assetReceiver(archiveAccount.getAddress())
                .assetIndex(timeslotIndex)
                .suggestedParams(params)
                .note(encodedTxMsg)
                .assetAmount(amount)
                .build();

        // admin send back receipts
        com.algorand.algosdk.transaction.Transaction receiptValidateTx = com.algorand.algosdk.transaction.Transaction
                .AssetTransferTransactionBuilder()
                .sender(adminAccount.getAddress())
                .assetReceiver(comsumerAccount.getAddress())
                .assetIndex(timeslotReceiptIndex)
                .suggestedParams(params)
                .note(encodedTxMsg)
                .assetAmount(amount)
                .build();

        // Group the transactions
        Digest gid = TxGroup.computeGroupID(ticketValidateTx, receiptValidateTx);
        ticketValidateTx.assignGroupID(gid);
        receiptValidateTx.assignGroupID(gid);

        SignedTransaction signedAssetTTx = comsumerAccount.signTransaction(ticketValidateTx);
        SignedTransaction signedAssetRTx = adminAccount.signTransaction(receiptValidateTx);

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        byte[] encodedTxBytes2 = Encoder.encodeToMsgPack(signedAssetTTx);
        byte[] encodedTxBytes3 = Encoder.encodeToMsgPack(signedAssetRTx);
        byteOutputStream.write(encodedTxBytes2);
        byteOutputStream.write(encodedTxBytes3);
        byte[] groupTransactionBytes = byteOutputStream.toByteArray();

        Response<PostTransactionsResponse> txResponse =
                algoService.getClient().RawTransaction().rawtxn(groupTransactionBytes).execute();

        String txId;
        if (txResponse.isSuccessful()) {
            txId = txResponse.body().txId;
            logger.info("Transaction id: ", txId);
            // write transaction to node
            algoService.waitForConfirmation(txId, 6);

        } else {
            //TODO manage the exception
            txId = "error";
        }

        //TODO return a java object
        return "{\"TxId\":\"" + txId + "\"}";
    }


    @PostMapping(value = "/ticket/{timeslotIndex}/consume/amount/{amount}", produces="application/json")
    @ResponseBody
    public String consumeTicket(@PathVariable long timeslotIndex, @PathVariable long amount,
                                 Principal principal) throws Exception{

        com.algorand.algosdk.account.Account comsumerAccount = algoService.getAccount(principal.getName());
        com.algorand.algosdk.account.Account archiveAccount = algoService.getArchiveAccount();

        // Check if the archive account has opted in
        algoService.checkOptIn(timeslotIndex, archiveAccount);

        //TODO check execute first
        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();

//        TxNoteMessage txNoteMessage = new TxNoteMessage(
//                "You miss 100% of the shots you don’t take",
//                "Wayne Gretzky");
        byte[] encodedTxMsg = Encoder.encodeToMsgPack("");

        // consumer consume timeslots
        com.algorand.algosdk.transaction.Transaction ticketTx = com.algorand.algosdk.transaction.Transaction
                .AssetTransferTransactionBuilder()
                .sender(comsumerAccount.getAddress())
                .assetReceiver(archiveAccount.getAddress())
                .assetIndex(timeslotIndex)
                .suggestedParams(params)
                .note(encodedTxMsg)
                .assetAmount(amount)
                .build();

        SignedTransaction signedTicket = comsumerAccount.signTransaction(ticketTx);

        byte[] encodedTicketTx = Encoder.encodeToMsgPack(signedTicket);

        Response<PostTransactionsResponse> txResponse =
                algoService.getClient().RawTransaction().rawtxn(encodedTicketTx).execute();

        String txId;
        if (txResponse.isSuccessful()) {
            txId = txResponse.body().txId;
            logger.info("Transaction id: ", txId);
            // write transaction to node
            algoService.waitForConfirmation(txId, 6);

        } else {
            //TODO manage the exception
            txId = "error";
        }

        //TODO Return a real asset
        return "{\"TxId\":\"" + txId + "\"}";
    }




}

