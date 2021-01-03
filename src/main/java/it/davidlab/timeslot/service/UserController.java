package it.davidlab.timeslot.service;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.TxGroup;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import it.davidlab.timeslot.domain.TimeslotProps;
import it.davidlab.timeslot.dto.AssetInfo;
import it.davidlab.timeslot.entity.AccountEntity;
import it.davidlab.timeslot.repository.AccountRepo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

@Api(value="User API")
@RestController
@RequestMapping("/timeslot")
public class UserController {

    private AlgoService algoService;
    private AccountRepo accountRepo;

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(AccountRepo accountRepo, AlgoService algoService) {
        this.accountRepo = accountRepo;
        this.algoService = algoService;
    }


    @GetMapping(path = "/info")
    @ResponseBody
    public List<AssetInfo> timeslotInfo(Principal principal) throws Exception {

        AccountEntity currentAccount = accountRepo.getByUsername(principal.getName());

        Address accAddress = new Address(currentAccount.getAddress());

        //TODO check execute() before call body()
        com.algorand.algosdk.v2.client.model.Account account =
                algoService.getClient().AccountInformation(accAddress).execute().body();

        List<AssetHolding> assets = account.assets;

        List<AssetInfo> assetInfo = new ArrayList<>();

        assets.stream().filter(a -> !StringUtils.isEmpty(a.creator)).forEach(a -> {
            Optional<AssetInfo> asset = getAssetProperties(a.assetId);
            asset.ifPresent(as -> {
                as.setAmount(a.amount.longValue());
                assetInfo.add(as);
            });
        });

        return assetInfo;
    }



    /**
     * User can buy one or more timeslots
     * @param timeslotIndex
     * @param amount
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/buy/{timeslotIndex}/amount/{amount}",produces="application/json")
    @ResponseBody
    public String buyTimeslot(@PathVariable Long timeslotIndex, @PathVariable int amount,
                              Principal principal) throws Exception {

        Optional<TimeslotProps> timeslotProps = getAssetParams(timeslotIndex);

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


    private Optional<AssetInfo> getAssetProperties(long assetId) {

        Optional<AssetInfo> optAssetProps = Optional.empty();

        Response<AssetResponse> assetResponse;
        try {
            assetResponse = algoService.getIndexerClient().lookupAssetByID(assetId).execute();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return optAssetProps;
        }

        if (assetResponse.isSuccessful()) {

            Optional<TimeslotProps> optTsprops = getAssetParams(assetId);

            if (optTsprops.isPresent()) {
                AssetParams assetParams = assetResponse.body().asset.params;

                TimeslotProps tsprops = optTsprops.get();

                AssetInfo assetInfo = new AssetInfo(assetId, assetParams.unitName, assetParams.name, assetParams.url,
                        0, tsprops.getStartValidity(), tsprops.getEndValidity(), tsprops.getDuration(),
                        tsprops.getTimeUnit(), tsprops.getDescription(), tsprops.getPrice(), tsprops.getType());

                optAssetProps = Optional.of(assetInfo);
            }
        }

        return optAssetProps;
    }


    private Optional<TimeslotProps> getAssetParams(long asset) {

        Optional<TimeslotProps> timeslotProps = Optional.empty();

        // search for the ACFG transactions
        Response<TransactionsResponse> txResponse;
        try {
            txResponse = algoService.getIndexerClient().searchForTransactions()
                        .assetId(asset).txType(Enums.TxType.ACFG).execute();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return timeslotProps;
        }

        if (txResponse.isSuccessful()) {
            List<com.algorand.algosdk.v2.client.model.Transaction> txs = txResponse.body().transactions;

            // get the last note field not null
            byte[] note = txs.stream().min(Comparator.comparingLong(t -> t.confirmedRound))
                    .map(transaction -> transaction.note).orElse(null);

            if (note != null) {
                try {
                    timeslotProps = Optional.of(Encoder.decodeFromMsgPack(note, TimeslotProps.class));
                } catch (IOException e) {
                    logger.error("It's not possible to decoded note for tx:" + txs.get(0).id);
                }
            }
        }
        return timeslotProps;
    }

}

