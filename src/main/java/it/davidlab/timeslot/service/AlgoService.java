package it.davidlab.timeslot.service;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.*;
import it.davidlab.timeslot.domain.TimeslotProps;
import it.davidlab.timeslot.domain.AssetInfo;
import it.davidlab.timeslot.domain.TxInfo;
import it.davidlab.timeslot.dao.AccountDao;
import it.davidlab.timeslot.repository.AccountRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Service()
public class AlgoService {

    @Value("${algorand.algod.address}")
    private String ALGOD_API_ADDR;
    @Value("${algorand.algod.port}")
    private Integer ALGOD_PORT;
    @Value("${algorand.algod.api-token}")
    private String ALGOD_API_TOKEN;

    @Value("${algorand.indexer.address}")
    private String INDEXER_API_ADDR;
    @Value("${algorand.indexer.port}")
    private int INDEXER_API_PORT;

    private AlgodClient client;
    private IndexerClient indexerClient;

    private AccountRepo accountRepo;

    private static final Logger logger = LoggerFactory.getLogger(AlgoService.class);

    public AlgoService(AccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    @PostConstruct
    public void init() {
        client = new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
        indexerClient = new IndexerClient(INDEXER_API_ADDR, INDEXER_API_PORT);
    }


    /**
     * Wait for transaction confirmation
     * @param txId
     * @param counter
     * @throws Exception
     */
    public void waitForConfirmation(String txId, int counter) throws Exception {
        long currentRound = client.GetStatus().execute().body().lastRound;
        long maxRound = currentRound + counter;

        Long txConfirmedRound = -1L;

        while (txConfirmedRound == null || txConfirmedRound < 0L) {
            Response<PendingTransactionResponse> response = client.PendingTransactionInformation(txId).execute();

            if (response.isSuccessful()) {
                txConfirmedRound = response.body().confirmedRound;
                if (txConfirmedRound == null) {
                    currentRound++;
                    if (currentRound > maxRound) {
                        throw new TimeoutException("Transaction not confirmed");
                    } else {
                        client.WaitForBlock(currentRound).execute();
                    }
                }
            }
            else {
                throw new IllegalStateException("Confirmation check failed");
            }
        }
    }

    public Optional<AssetInfo> getAssetProperties(long assetId) {
        return getAssetProperties(assetId, -1);
    }

    public Optional<AssetInfo> getAssetProperties(long assetId, long assetAmount) {

        Optional<AssetInfo> optAssetProps = Optional.empty();

        Response<AssetResponse> assetResponse;
        try {
            assetResponse = indexerClient.lookupAssetByID(assetId).execute();
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
                        assetAmount, tsprops.getStartValidity(), tsprops.getEndValidity(), tsprops.getDuration(),
                        tsprops.getTimeUnit(), tsprops.getDescription(), tsprops.getTsLocation(),
                        tsprops.getPrice(), tsprops.getType());

                optAssetProps = Optional.of(assetInfo);
            }
        }

        return optAssetProps;
    }


    public Optional<TimeslotProps> getAssetParams(long asset) {

        Optional<TimeslotProps> timeslotProps = Optional.empty();

        // search for the ACFG transactions
        Response<TransactionsResponse> txResponse;
        try {
            com.algorand.algosdk.account.Account adminAccount = getAdminAccount();

            txResponse = indexerClient.searchForTransactions()
                    .address(adminAccount.getAddress()).addressRole(Enums.AddressRole.SENDER)
                    .assetId(asset).txType(Enums.TxType.ACFG).execute();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return timeslotProps;
        }

        if (txResponse.isSuccessful()) {
            List<Transaction> txs = txResponse.body().transactions;

            // get the last note field not null
            byte[] note = txs.stream().min(Comparator.comparingLong(t -> t.confirmedRound))
                    .map(transaction -> transaction.note).orElse(null);

            if (note != null) {
                try {
                    timeslotProps = Optional.of(Encoder.decodeFromMsgPack(note, TimeslotProps.class));
                } catch (IOException e) {
                    logger.error("It's not possible to decode note for tx:" + txs.get(0).id);
                }
            }
        }
        return timeslotProps;
    }


    public TxInfo getTxParams(Transaction t, long ticketId) {

        return new TxInfo(t.id, ticketId, t.assetTransferTransaction.amount.longValue(),
                t.sender, t.assetTransferTransaction.receiver, t.roundTime, "");
    }

    public com.algorand.algosdk.account.Account getAccount(String accountName) throws GeneralSecurityException {
        AccountDao consumerAccount = accountRepo.getByUsername(accountName);

        //TODO Check if the user exists
        return new com.algorand.algosdk.account.Account(consumerAccount.getPassphrase());
    }


    public com.algorand.algosdk.account.Account getAdminAccount() throws GeneralSecurityException {
        AccountDao consumerAccount = accountRepo.getByUsername("admin");
        return new com.algorand.algosdk.account.Account(consumerAccount.getPassphrase());
    }

    public com.algorand.algosdk.account.Account getArchiveAccount() throws GeneralSecurityException {
        AccountDao consumerAccount = accountRepo.getByUsername("archive");
        return new com.algorand.algosdk.account.Account(consumerAccount.getPassphrase());
    }

    public Address getAccountAddress(String accountName) throws GeneralSecurityException {
        return getAccount(accountName).getAddress();
    }

    public Address getAdminAddress() throws GeneralSecurityException {
        return getAdminAccount().getAddress();
    }

    public Address getArchiveAddress() throws GeneralSecurityException {
        return getArchiveAccount().getAddress();
    }


    /**
     * Check if an account has opted-in for an asset
     * @param assetId
     * @param algoAccount
     * @throws Exception
     */
    public void checkOptIn(Long assetId, com.algorand.algosdk.account.Account algoAccount) throws Exception {
        // check if receiver has opted in for the asset
        Response<com.algorand.algosdk.v2.client.model.Account> accountResponse =
                getClient().AccountInformation(algoAccount.getAddress()).execute();

        if (accountResponse.isSuccessful()) {
            List<AssetHolding> assets = accountResponse.body().assets;
            if (!assets.stream().filter(a -> a.assetId.equals(assetId)).findFirst().isPresent()) {
                optIn(assetId, algoAccount);
            }
        }
    }

    public String optIn(long assetIndex, com.algorand.algosdk.account.Account optinAccount) throws Exception {

        //TODO check execute() before calling body()
        TransactionParametersResponse params = getClient().TransactionParams().execute().body();
        params.fee = 1000L;

        com.algorand.algosdk.transaction.Transaction tx = com.algorand.algosdk.transaction.Transaction
                .AssetAcceptTransactionBuilder()
                .acceptingAccount(optinAccount.getAddress())
                .assetIndex(assetIndex)
                .suggestedParams(params)
                .build();

        SignedTransaction signedTx = optinAccount.signTransaction(tx);
        String txId = getClient().RawTransaction().rawtxn(Encoder.encodeToMsgPack(signedTx)).execute().body().txId;

        waitForConfirmation(txId, 6);

        return txId;
    }


    public void sendTransaction(com.algorand.algosdk.account.Account algoAccount,
                                com.algorand.algosdk.transaction.Transaction purchaseTx) throws Exception {
        SignedTransaction signedTicket = algoAccount.signTransaction(purchaseTx);

        byte[] encodedTicketTx = Encoder.encodeToMsgPack(signedTicket);

        Response<PostTransactionsResponse> txResponse =
                client.RawTransaction().rawtxn(encodedTicketTx).execute();

        String txId;
        if (txResponse.isSuccessful()) {
            txId = txResponse.body().txId;
            logger.info("Transaction id: ", txId);
            // write transaction to node
            waitForConfirmation(txId, 6);
        }
        else {
            throw new Exception("Transaction Error");
        }
    }



    public AlgodClient getClient() {
        return client;
    }

    public IndexerClient getIndexerClient() {
        return indexerClient;
    }
}
