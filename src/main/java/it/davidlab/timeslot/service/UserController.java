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
    private AccountRepo accountRepo;

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(AccountRepo accountRepo, AlgoService algoService) {
        this.accountRepo = accountRepo;
        this.algoService = algoService;
    }


    @GetMapping(path = "ticket/avaiable", produces="application/json")
    @ResponseBody
    public List<AssetInfo> ticketAvailable() throws Exception {

        AccountEntity currentAccount = accountRepo.getByUsername("admin");

        Address accAddress = new Address(currentAccount.getAddress());

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(accAddress).execute().body();

        List<AssetHolding> assets = account.assets;

        List<AssetInfo> assetInfo = assets.stream().filter(a -> !StringUtils.isEmpty(a.creator))
                .map(a-> algoService.getAssetProperties(a.assetId, a.amount.longValue()))
                .filter(a -> a.isPresent() && a.get().getType() == AssetType.TICKET && a.get().getAmount()>0)
                .map(Optional::get).collect(Collectors.toList());

        return assetInfo;
    }


    @GetMapping(path = "/ticket/owned", produces="application/json")
    @ResponseBody
    public List<AssetInfo> ticketOwned(Principal principal) throws Exception {

        AccountEntity currentAccount = accountRepo.getByUsername(principal.getName());

        Address accAddress = new Address(currentAccount.getAddress());

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(accAddress).execute().body();

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

        AccountEntity currentAccount = accountRepo.getByUsername(principal.getName());

        Address accAddress = new Address(currentAccount.getAddress());

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(accAddress).execute().body();

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

        AccountEntity currentAccount = accountRepo.getByUsername(principal.getName());

        Address currentAccountAddr = new Address(currentAccount.getAddress());

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(currentAccountAddr).execute().body();

        List<com.algorand.algosdk.v2.client.model.Transaction> txs = algoService.getIndexerClient()
                .lookupAccountTransactions(currentAccountAddr).txType(Enums.TxType.AXFER)
                .assetId(ticketId).execute().body().transactions;

        List<TxInfo> txInfos = new ArrayList<>(txs.size());

        txs.forEach(t -> txInfos.add(algoService.getTxParams(t, ticketId)));

        return txInfos;
    }


    @GetMapping(path = "/ticket/{ticketid}/details", produces="application/json")
    @ResponseBody
    public AssetInfo ticketDetails(@PathVariable long ticketid, Principal principal) throws Exception {

        AccountEntity currentAccount = accountRepo.getByUsername("admin");

        Address accAddress = new Address(currentAccount.getAddress());

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(accAddress).execute().body();

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
        AccountEntity buyerAccount = accountRepo.getByUsername(principal.getName());

        com.algorand.algosdk.account.Account buyerAlgoAccount =
                new com.algorand.algosdk.account.Account(buyerAccount.getPassphrase());

        //TODO make "admin" parametric
        AccountEntity adminAccount = accountRepo.getByUsername("admin");

        com.algorand.algosdk.account.Account adminAlgoAccount =
                new com.algorand.algosdk.account.Account(adminAccount.getPassphrase());

        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();

        checkOptIn(timeslotIndex, buyerAlgoAccount);

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

        SignedTransaction signedPurchaseTx = buyerAlgoAccount.signTransaction(purchaseTx);
        SignedTransaction signedAssetTTx = adminAlgoAccount.signTransaction(assetTTx);

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




    @PostMapping(value = "/{timeslotIndex}/consume/amount/{amount}", produces="application/json")
    @ResponseBody
    public String validateTicket(@PathVariable long timeslotIndex, @PathVariable long amount,
                                  Principal principal) throws Exception{

        AccountEntity consumerAccount = accountRepo.getByUsername(principal.getName());

        com.algorand.algosdk.account.Account comsumerAlgoAccount =
                new com.algorand.algosdk.account.Account(consumerAccount.getPassphrase());

        AccountEntity adminAccount = accountRepo.getByUsername("admin");
        com.algorand.algosdk.account.Account adminAlgoAccount =
                new com.algorand.algosdk.account.Account(adminAccount.getPassphrase());

        //TODO check execute first
        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();

        //TODO find a smarter way to retrieve timeslotIndex+1
        long timeslotReceiptIndex = timeslotIndex + 1;
        checkOptIn(timeslotReceiptIndex, comsumerAlgoAccount);

//        TxNoteMessage txNoteMessage = new TxNoteMessage(
//                "You miss 100% of the shots you don’t take",
//                "Wayne Gretzky");
        byte[] encodedTxMsg = Encoder.encodeToMsgPack("");

        // consumer consume timeslots
        com.algorand.algosdk.transaction.Transaction assetTTx = com.algorand.algosdk.transaction.Transaction
                .AssetTransferTransactionBuilder()
                .sender(comsumerAlgoAccount.getAddress())
                .assetReceiver(adminAccount.getAddress())
                .assetIndex(timeslotIndex)
                .suggestedParams(params)
                .note(encodedTxMsg)
                .assetAmount(amount)
                .build();

        // admin send back receipts
        com.algorand.algosdk.transaction.Transaction assetRTx = com.algorand.algosdk.transaction.Transaction
                .AssetTransferTransactionBuilder()
                .sender(adminAccount.getAddress())
                .assetReceiver(comsumerAlgoAccount.getAddress())
                .assetIndex(timeslotReceiptIndex)
                .suggestedParams(params)
                .note(encodedTxMsg)
                .assetAmount(amount)
                .build();

        // Group the transactions
        Digest gid = TxGroup.computeGroupID(assetTTx, assetRTx);
        assetTTx.assignGroupID(gid);
        assetRTx.assignGroupID(gid);

        SignedTransaction signedAssetTTx = comsumerAlgoAccount.signTransaction(assetTTx);
        SignedTransaction signedAssetRTx = adminAlgoAccount.signTransaction(assetRTx);

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

        return "{\"TxId\":\"" + txId + "\"}";
    }



    /*------------------
        Private Methods
     -------------------*/

    /**
     * Check if an account has opted-in for an asset
     * @param assetId
     * @param algoAccount
     * @throws Exception
     */
    private void checkOptIn(Long assetId, com.algorand.algosdk.account.Account algoAccount) throws Exception {
        // check if receiver has opted in for the asset
        Response<com.algorand.algosdk.v2.client.model.Account> accountResponse =
                algoService.getClient().AccountInformation(algoAccount.getAddress()).execute();

        if (accountResponse.isSuccessful()) {
            List<AssetHolding> assets = accountResponse.body().assets;
            if (!assets.stream().filter(a -> a.assetId.equals(assetId)).findFirst().isPresent()) {
                optIn(assetId, algoAccount);
            }
        }
    }

    private String optIn(long assetIndex, com.algorand.algosdk.account.Account optinAccount) throws Exception {

        //TODO check execute() before calling body()
        TransactionParametersResponse params = algoService.getClient().TransactionParams().execute().body();
        params.fee = 1000L;

        com.algorand.algosdk.transaction.Transaction tx = com.algorand.algosdk.transaction.Transaction
                .AssetAcceptTransactionBuilder()
                .acceptingAccount(optinAccount.getAddress())
                .assetIndex(assetIndex)
                .suggestedParams(params)
                .build();

        SignedTransaction signedTx = optinAccount.signTransaction(tx);
        String txId = algoService.getClient().RawTransaction().rawtxn(Encoder.encodeToMsgPack(signedTx))
                .execute().body().txId;

        algoService.waitForConfirmation(txId, 6);

        return txId;
    }




}

