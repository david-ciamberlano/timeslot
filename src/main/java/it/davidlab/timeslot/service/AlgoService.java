package it.davidlab.timeslot.service;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.*;
import it.davidlab.timeslot.domain.TimeslotProperties;
import it.davidlab.timeslot.domain.TimeslotDto;
import it.davidlab.timeslot.domain.TransactionDto;
import it.davidlab.timeslot.dao.AccountDao;
import it.davidlab.timeslot.repository.AccountRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
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

    @Value("${timeslot.admin.user}")
    private String adminUser;

    @Value("${timeslot.archive.user}")
    private String archiveUser;

    private AlgodClient client;
    private IndexerClient indexerClient;

    private AccountRepo accountRepo;

    private final Logger logger = LoggerFactory.getLogger(AlgoService.class);

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
     *
     * @param txId
     * @param timeout
     * @throws Exception
     */
    public void waitForConfirmation(String txId, int timeout) throws Exception {

        Long txConfirmedRound = -1L;
        Response<NodeStatusResponse> statusResponse = client.GetStatus().execute();

        long lastRound;
        if (statusResponse.isSuccessful()) {
            lastRound = statusResponse.body().lastRound + 1L;
        }
        else {
            throw new IllegalStateException("Cannot get node status");
        }

        long maxRound = lastRound + timeout;

        for (long currentRound = lastRound; currentRound < maxRound; currentRound++) {
            Response<PendingTransactionResponse> response = client.PendingTransactionInformation(txId).execute();

            if (response.isSuccessful()) {
                txConfirmedRound = response.body().confirmedRound;
                if (txConfirmedRound == null) {
                    if (!client.WaitForBlock(currentRound).execute().isSuccessful()) {
                        throw new Exception();
                    }
                }
                else {
                    return;
                }
            } else {
                throw new IllegalStateException("The transaction has been rejected");
            }
        }

        throw new IllegalStateException("Transaction not confirmed after " + timeout + " rounds!");
    }



    public Optional<TimeslotDto> getAssetProperties(long assetId) {
        return getAssetProperties(assetId, -1);
    }


    public Optional<TimeslotDto> getAssetProperties(long assetId, long assetAmount) {

        Optional<TimeslotDto> optAssetProps = Optional.empty();

        Response<AssetResponse> assetResponse;
        try {
            assetResponse = indexerClient.lookupAssetByID(assetId).execute();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return optAssetProps;
        }

        if (assetResponse.isSuccessful()) {
            Optional<TimeslotProperties> optTsprops = getAssetParams(assetId);

            if (optTsprops.isPresent()) {
                AssetParams assetParams = assetResponse.body().asset.params;

                TimeslotProperties tsprops = optTsprops.get();
                TimeslotDto timeslotDto = new TimeslotDto(assetId, assetParams.unitName, assetParams.name, assetParams.url,
                        assetAmount, tsprops.getStartValidity(), tsprops.getEndValidity(), tsprops.getDuration(),
                        tsprops.getTimeslotUnit(), tsprops.getDescription(), tsprops.getTimeslotLocation(),
                        tsprops.getPrice(), tsprops.getTimeslotType());

                optAssetProps = Optional.of(timeslotDto);
            }
        }

        return optAssetProps;
    }


    public Optional<TimeslotProperties> getAssetParams(long asset) {

        Optional<TimeslotProperties> timeslotProps = Optional.empty();

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
                    timeslotProps = Optional.of(Encoder.decodeFromMsgPack(note, TimeslotProperties.class));
                } catch (IOException e) {
                    logger.error("It's not possible to decode note for tx:" + txs.get(0).id);
                }
            }
        }
        return timeslotProps;
    }


    public TransactionDto getTxParams(Transaction t, long timeslotId) {

        String note = t.note() != null ? new String(Encoder.decodeFromBase64(t.note()), StandardCharsets.UTF_8) : "";

        AccountDao senderAccount = accountRepo.getByAddress(t.sender);
        AccountDao receiverAccount = accountRepo.getByAddress(t.assetTransferTransaction.receiver);

        TransactionDto transactionDto = new TransactionDto(t.id, timeslotId,
                t.assetTransferTransaction.amount.longValue(), senderAccount.getUsername(),
                receiverAccount.getUsername(), t.roundTime, note);

        return transactionDto;
    }

    public com.algorand.algosdk.account.Account getAccount(String accountName) throws GeneralSecurityException {
        AccountDao consumerAccount = accountRepo.getByUsername(accountName);

        //TODO Check if the user exists
        return new com.algorand.algosdk.account.Account(consumerAccount.getPassphrase());
    }

    public AccountDao getNewAccount(String username) throws Exception{
        com.algorand.algosdk.account.Account newAccount = new com.algorand.algosdk.account.Account();

        return new AccountDao(username,
                newAccount.getAddress().toString(), newAccount.toMnemonic());
    }


    public com.algorand.algosdk.account.Account getAdminAccount() throws GeneralSecurityException {
        AccountDao consumerAccount = accountRepo.getByUsername(adminUser);
        return new com.algorand.algosdk.account.Account(consumerAccount.getPassphrase());
    }

    public com.algorand.algosdk.account.Account getArchiveAccount() throws GeneralSecurityException {
        AccountDao consumerAccount = accountRepo.getByUsername(archiveUser);
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


    public Account getAccountModel(Address address) throws Exception {

        Response<Account> accountResponse = client.AccountInformation(address).execute();

        if (accountResponse.isSuccessful()) {
            return accountResponse.body();
        }
        else {
            //TODO Custom Exception
            throw new Exception();
        }
    }

    public List<AssetHolding> getAccountAssets(Address address) throws Exception {
        return getAccountModel(address).assets;
    }

    public TransactionParametersResponse getTxParams() throws Exception {

        Response<TransactionParametersResponse> txParResp = client.TransactionParams().execute();

        if (txParResp.isSuccessful()) {
            return txParResp.body();
        }
        else {
            //TODO Custom Exception
            throw new Exception();
        }

    }


    /**
     * Check if an account has opted-in for an asset
     *
     * @param assetId
     * @param algoAccount
     * @throws Exception
     */
    public void checkOptIn(Long assetId, com.algorand.algosdk.account.Account algoAccount) throws Exception {
        // check if receiver has opted in for the asset
        Response<com.algorand.algosdk.v2.client.model.Account> accountResponse =
                client.AccountInformation(algoAccount.getAddress()).execute();

        if (accountResponse.isSuccessful()) {
            List<AssetHolding> assets = accountResponse.body().assets;
            if (assets.stream().noneMatch(a -> a.assetId.equals(assetId))) {
                optIn(assetId, algoAccount);
            }
        }
    }

    public String optIn(long assetIndex, com.algorand.algosdk.account.Account optinAccount) throws Exception {

        Response<TransactionParametersResponse> txParamResponse =client.TransactionParams().execute();

        if (!txParamResponse.isSuccessful()) {
            //TODO Custom exception
            throw new Exception();
        }

        TransactionParametersResponse params = txParamResponse.body();
        params.fee = 1000L;

        com.algorand.algosdk.transaction.Transaction tx = com.algorand.algosdk.transaction.Transaction
                .AssetAcceptTransactionBuilder()
                .acceptingAccount(optinAccount.getAddress())
                .assetIndex(assetIndex)
                .suggestedParams(params)
                .build();

        SignedTransaction signedTx = optinAccount.signTransaction(tx);
        //TODO check execute
        Response<PostTransactionsResponse> txResponse =
                client.RawTransaction().rawtxn(Encoder.encodeToMsgPack(signedTx)).execute();

        String txId;
        if (txResponse.isSuccessful()) {
            txId = txResponse.body().txId;
            logger.info("Transaction id: ", txId);
            // write transaction to node
            waitForConfirmation(txId, 6);
        } else {
            //TODO Custom Exception
            throw new Exception("Transaction Error");
        }

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
        } else {
            //TODO Custom Exception
            throw new Exception("Transaction Error");
        }
    }

    /**
     * Send Algo to an account
     * @param receiverAccount
     * @param amount
     * @throws Exception
     */
    public void sendAlgo(com.algorand.algosdk.account.Account receiverAccount, long amount) throws Exception {

        long mAlgoAmount = amount * 1000000L; //converted in microAlgorand

        String note = "Hello World";
        TransactionParametersResponse params = client.TransactionParams().execute().body();
        com.algorand.algosdk.transaction.Transaction tx =
                com.algorand.algosdk.transaction.Transaction.PaymentTransactionBuilder()
                .sender(getAdminAddress())
                .note(note.getBytes())
                .amount(mAlgoAmount)
                .receiver(receiverAccount.getAddress())
                .suggestedParams(params)
                .build();

        SignedTransaction signedTx = getAdminAccount().signTransaction(tx);
        //TODO check execute
        Response<PostTransactionsResponse> txResponse =
                client.RawTransaction().rawtxn(Encoder.encodeToMsgPack(signedTx)).execute();

        String txId;
        if (txResponse.isSuccessful()) {
            txId = txResponse.body().txId;
            logger.info("Transaction id: ", txId);
            // write transaction to node
            waitForConfirmation(txId, 6);
        } else {
            //TODO Custom Exception
            throw new Exception("Transaction Error");
        }

    }


    public List<com.algorand.algosdk.v2.client.model.Transaction>
                        getAssetsTransaction(long assetId) throws Exception {

        Response<TransactionsResponse> txResponse = getIndexerClient().lookupAssetTransactions(assetId)
                    .txType(Enums.TxType.AXFER).execute();

        List<com.algorand.algosdk.v2.client.model.Transaction> txs;

        if (txResponse.isSuccessful()) {
            return txResponse.body().transactions;

        } else {
            //TODO Custom Exception
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
