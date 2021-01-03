package it.davidlab.timeslot.service;

import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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
    public void init() {
        client = new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
        indexerClient = new IndexerClient(INDEXER_API_ADDR, INDEXER_API_PORT);
    }


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


    public AlgodClient getClient() {
        return client;
    }

    public IndexerClient getIndexerClient() {
        return indexerClient;
    }
}
