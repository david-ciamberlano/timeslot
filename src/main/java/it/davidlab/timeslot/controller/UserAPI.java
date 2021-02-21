package it.davidlab.timeslot.controller;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.transaction.TxGroup;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.AssetHolding;
import com.algorand.algosdk.v2.client.model.Enums;
import com.algorand.algosdk.v2.client.model.PostTransactionsResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import it.davidlab.timeslot.domain.*;
import it.davidlab.timeslot.service.AlgoService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Api(value = "User API")
@RestController
@RequestMapping("/user")
public class UserAPI {

    private AlgoService algoService;

    private final Logger logger = LoggerFactory.getLogger(UserAPI.class);

    public UserAPI(AlgoService algoService) {
        this.algoService = algoService;
    }


    /**
     * get the available/owned timeslots
     *
     * @param principal
     * @return
     * @throws Exception
     */
    @Operation(summary = "get timeslot list {available | owned}")
    @GetMapping(path = "/v1/timeslots", produces = "application/json")
    @ResponseBody
    public List<TimeslotDto> timeslotList(@RequestParam String filter,
                                          @RequestParam(required = false) Optional<String> namePrefix,
                                          Principal principal) throws Exception {

        Address accountAddress;

        // at least one between 'available' and 'owned' is required
        switch (filter) {
            case "available":
                accountAddress = algoService.getAdminAddress();
                break;

            case "owned":
                accountAddress = algoService.getAccountAddress(principal.getName());
                break;

            default:
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wrong Filter" );
        }

        List<AssetHolding> assets = algoService.getAccountAssets(accountAddress);

        List<TimeslotDto> timeslotDto = assets.stream().filter(a -> !StringUtils.isEmpty(a.creator))
                .map(a -> algoService.getAssetProperties(a.assetId, a.amount.longValue()))
                .filter(a -> a.isPresent() && a.get().getType() == TimeslotType.TICKET && a.get().getAmount() > 0)
                .filter(a -> a.get().getUnitName().startsWith(namePrefix.orElse("")))
                .map(Optional::get).collect(Collectors.toList());

        return timeslotDto;
    }


    /**
     * get the owned timeslotpairs
     *
     * @param principal
     * @return
     * @throws Exception
     */
    @Operation(summary = "Get timeslotpairs owned")
    @GetMapping(path = "/v1/timeslotpairs", produces = "application/json")
    @ResponseBody
    public List<TimeslotDto> timeslotpairsList(@RequestParam String f,
                                               @RequestParam(required = false) Optional<String> namePrefix,
                                               Principal principal) throws Exception {

        Address accountAddress;

       switch (f) {
           case "available":
            accountAddress = algoService.getAdminAddress();
           break;
           case "owned":
            accountAddress = algoService.getAccountAddress(principal.getName());
            break;
           default:
               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong filter" );
        }

        List<AssetHolding> assets = algoService.getAccountAssets(accountAddress);

        List<TimeslotDto> timeslotDto = assets.stream().filter(a -> !StringUtils.isEmpty(a.creator))
                .map(a -> algoService.getAssetProperties(a.assetId, a.amount.longValue()))
                .filter(p -> p.isPresent()
                        && (p.get().getType() == TimeslotType.PAIR || p.get().getType() == TimeslotType.RECEIPT)
                        && p.get().getAmount() > 0)
                .filter(p -> p.get().getUnitName().startsWith(namePrefix.orElse("")))
                .map(Optional::get).collect(Collectors.toList());

        return timeslotDto;

    }


    @Operation(summary = "Get transactions of the current user")
    @GetMapping(path = "/v1/timeslots/{id}/transactions", produces = "application/json")
    @ResponseBody
    public List<TransactionDto> getTransactions(@PathVariable long id,
                                                @RequestParam(required = false) String receiver,
                                                @RequestParam(required = false) String noteprefix,
                                                Principal principal) throws Exception {

        com.algorand.algosdk.account.Account currentAccount = algoService.getAccount(principal.getName());

        List<com.algorand.algosdk.v2.client.model.Transaction> txs = algoService.getIndexerClient()
                .lookupAccountTransactions(currentAccount.getAddress()).txType(Enums.TxType.AXFER)
                .assetId(id).execute().body().transactions;


        List<TransactionDto> transactionDtos = txs.stream()
                .map(t -> algoService.getTxParams(t, id))
                .filter(t -> t.getAmount() > 0)
                .filter(t -> noteprefix != null ? t.getNote().startsWith(noteprefix) : true)
                .filter(t -> receiver != null ? t.getReceiverUser().equals(receiver) : true)
                .collect(Collectors.toList());

        return transactionDtos;
    }


    @Operation(summary = "get timeslot details")
    @GetMapping(path = "/v1/timeslots/{id}", produces = "application/json")
    @ResponseBody
    public TimeslotDto timeslotDetails(@PathVariable long id) {

        TimeslotDto timeslotDto = algoService.getAssetProperties(id).orElseThrow();

        return timeslotDto;
    }


    /**
     * User can buy one or more timeslots
     *
     * @param id
     * @param amount
     * @return
     * @throws Exception
     */
    @Operation(summary = "take timeslots")
    @PostMapping(value = "/v1/timeslots/{id}/take/{amount}", produces = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ResponseBody
    public void takeTimeslots(@PathVariable Long id, @PathVariable int amount,
                              @RequestParam(required = false) String note,
                              Principal principal) throws Exception {

        Optional<TimeslotProperties> timeslotProps = algoService.getAssetParams(id);

        long price;
        if (timeslotProps.isPresent()) {
            price = timeslotProps.get().getPrice();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot Properties Not Found");
        }

        com.algorand.algosdk.account.Account buyerAccount = algoService.getAccount(principal.getName());
        com.algorand.algosdk.account.Account adminAccount = algoService.getAdminAccount();

        algoService.checkOptIn(id, buyerAccount);
        long totalBookingPrice = (price * 1000000L) * (long) amount; //converted in microAlgorand

        TransactionParametersResponse params = algoService.getTxParams();

        com.algorand.algosdk.transaction.Transaction purchaseTx = com.algorand.algosdk.transaction.Transaction.PaymentTransactionBuilder()
                .sender(buyerAccount.getAddress())
                .amount(totalBookingPrice)
                .receiver(adminAccount.getAddress())
                .suggestedParams(params)
                .build();

        byte[] encodedTxMsg = Encoder.encodeToMsgPack(note);
        com.algorand.algosdk.transaction.Transaction assetTTx = com.algorand.algosdk.transaction.Transaction
                .AssetTransferTransactionBuilder()
                .sender(adminAccount.getAddress())
                .assetReceiver(buyerAccount.getAddress())
                .assetIndex(id)
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
            String txId = txResponse.body().txId;
            logger.info("Transaction id: ", txId);
            // write transaction to node
            algoService.waitForConfirmation(txId, 6);

        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Can't create the asset");
        }

    }


    @Operation(summary = "spend timeslots")
    @PostMapping(value = "/v1/timeslots/{id}/spend/{amount}", produces = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ResponseBody
    public void spendTimeslot(@PathVariable long id, @PathVariable long amount, @RequestParam String note,
                              Principal principal) throws Exception {

        com.algorand.algosdk.account.Account comsumerAccount = algoService.getAccount(principal.getName());
        com.algorand.algosdk.account.Account archiveAccount = algoService.getArchiveAccount();

        // Check if the archive account has opted in
        algoService.checkOptIn(id, archiveAccount);

        TransactionParametersResponse params = algoService.getTxParams();

        // consumer consume timeslots
        com.algorand.algosdk.transaction.Transaction ticketTx = Transaction
                .AssetTransferTransactionBuilder()
                .sender(comsumerAccount.getAddress())
                .assetReceiver(archiveAccount.getAddress())
                .assetIndex(id)
                .suggestedParams(params)
                .note(note.getBytes(StandardCharsets.UTF_8))
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction Error: " + txResponse.message());
        }

    }


    @Operation(summary = "spend timeslotpairs")
    @PostMapping(value = "/v1/timeslotpairs/{id}/spend/{amount}", produces = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ResponseBody
    public void spendTimeslotpairs(@PathVariable long id, @PathVariable long amount,
                                   @RequestParam(required = false) Optional<String> note,
                                   Principal principal) throws Exception {

        com.algorand.algosdk.account.Account comsumerAccount = algoService.getAccount(principal.getName());
        com.algorand.algosdk.account.Account adminAccount = algoService.getAdminAccount();
        com.algorand.algosdk.account.Account archiveAccount = algoService.getArchiveAccount();

        // check if the asset type is a Pair!
        // this could be done via Smartcontract... in a (near) future
        Optional<TimeslotProperties> timeslotProps = algoService.getAssetParams(id);

        if(!(timeslotProps.isPresent() && timeslotProps.get().getTimeslotType()==TimeslotType.PAIR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset is not a pair" );
        }

        algoService.checkOptIn(id, archiveAccount);

        //TODO find a smarter way to retrieve timeslotIndex+1
        long timeslotReceiptIndex = id + 1;
        algoService.checkOptIn(timeslotReceiptIndex, comsumerAccount);

        TransactionParametersResponse params = algoService.getTxParams();

        String txnote = note.orElse("");

        // consumer consume timeslots
        com.algorand.algosdk.transaction.Transaction ticketValidateTx = Transaction
                .AssetTransferTransactionBuilder()
                .sender(comsumerAccount.getAddress())
                .assetReceiver(archiveAccount.getAddress())
                .assetIndex(id)
                .suggestedParams(params)
                .note(txnote.getBytes(StandardCharsets.UTF_8))
                .assetAmount(amount)
                .build();

        // admin send back receipts
        com.algorand.algosdk.transaction.Transaction receiptValidateTx = Transaction
                .AssetTransferTransactionBuilder()
                .sender(adminAccount.getAddress())
                .assetReceiver(comsumerAccount.getAddress())
                .assetIndex(timeslotReceiptIndex)
                .suggestedParams(params)
                .note(txnote.getBytes(StandardCharsets.UTF_8))
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Error:" + txResponse.message());
        }

    }


}

