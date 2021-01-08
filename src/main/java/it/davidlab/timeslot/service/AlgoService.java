package it.davidlab.timeslot.service;

import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.*;
import it.davidlab.timeslot.domain.TimeslotProps;
import it.davidlab.timeslot.dto.AssetInfo;
import it.davidlab.timeslot.dto.TxInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Service()
class AlgoService {

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

    private static final Logger logger = LoggerFactory.getLogger(AlgoService.class);


    protected AlgoService() {
    }

    @PostConstruct
    protected void init() {
        client = new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
        indexerClient = new IndexerClient(INDEXER_API_ADDR, INDEXER_API_PORT);
    }


    /**
     * Wait for transaction confirmation
     * @param txId
     * @param counter
     * @throws Exception
     */
    protected void waitForConfirmation(String txId, int counter) throws Exception {
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

    protected Optional<AssetInfo> getAssetProperties(long assetId) {
        return getAssetProperties(assetId, -1);
    }

    protected Optional<AssetInfo> getAssetProperties(long assetId, long assetAmount) {

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
                        tsprops.getTimeUnit(), tsprops.getDescription(), tsprops.getPrice(), tsprops.getType());

                optAssetProps = Optional.of(assetInfo);
            }
        }

        return optAssetProps;
    }


    protected Optional<TimeslotProps> getAssetParams(long asset) {

        Optional<TimeslotProps> timeslotProps = Optional.empty();

        // search for the ACFG transactions
        Response<TransactionsResponse> txResponse;
        try {
            txResponse = indexerClient.searchForTransactions()
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
                    logger.error("It's not possible to decoded note for tx:" + txs.get(0).id);
                }
            }
        }
        return timeslotProps;
    }


    TxInfo getTxParams(Transaction t, long ticketId) {

        return new TxInfo(t.id, ticketId, t.assetTransferTransaction.amount.longValue(),
                t.sender, t.assetTransferTransaction.receiver, t.roundTime, "");
    }



    public AlgodClient getClient() {
        return client;
    }

    public IndexerClient getIndexerClient() {
        return indexerClient;
    }
}
