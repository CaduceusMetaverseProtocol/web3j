package com.cmp.signDemo;

import com.cmp.util.CustomizeHttpService;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;
import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * @ClassName SignAndSubmit
 * @Author Dirk
 * @Date 2022/8/22 17:58
 * @Version 1.0
 */
public class SignAndSubmit {

    private static Web3j web3j;

    private SignAndSubmit(String rpcUrl) {
        if (Objects.isNull(web3j)){
            web3j = Web3j.build(new CustomizeHttpService(rpcUrl));
        }
    }

    private void submitSign(byte[] signedMessage) {
        try {
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
            String transactionHash = ethSendTransaction.getTransactionHash();
            System.out.println("transactionHash:" + transactionHash);
            if (ethSendTransaction.hasError()) {
                String message = ethSendTransaction.getError().getMessage();
                System.out.println("transaction failed,info:" + message);
            } else {
                EthGetTransactionReceipt send = web3j.ethGetTransactionReceipt(transactionHash).send();
                if (send != null) {
                    System.out.println("transaction success");
                }
            }
        } catch (Exception e) {
            if (e instanceof ExecutionException || e instanceof InterruptedException) {
                System.out.println("----------get Nonce exception-----------");
            }
            e.printStackTrace();
        }
    }

    private BigInteger getAddressNonce(String privateKey) {
        try {
            String fromAddress = Keys.toChecksumAddress(
                    Keys.getAddress(
                            ECKeyPair.create(
                                    Numeric.toBigInt(privateKey))));
            System.out.println("fromAddress:" + fromAddress);
            return web3j.ethGetTransactionCount(
                    fromAddress, DefaultBlockParameterName.LATEST).send().getTransactionCount();
        } catch (Exception e) {
            e.printStackTrace();
            return BigInteger.valueOf(-1);
        }
    }

    private byte[] offlineSign(String fromPk, String toAddress, BigInteger value) {
        try {
            BigInteger nonce = getAddressNonce(fromPk);
            System.out.println("Nonce:" + nonce);
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger gasLimit = new BigInteger("900000");
            if (fromPk.startsWith("0x")) {
                fromPk = fromPk.substring(2);
            }
            Credentials credentials = Credentials.create(fromPk);
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    toAddress,
                    value,
                    "");
            System.out.println("toAddress:" + toAddress);
            return TransactionEncoder.signMessage(rawTransaction, 256256, credentials);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean signAndSend(String fromPk, String toAddress, BigInteger value) {
        try {
            BigInteger nonce = getAddressNonce(fromPk);
            System.out.println("Nonce:" + nonce);
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger gasLimit = new BigInteger("900000");
            Credentials credentials = Credentials.create(fromPk);
            RawTransactionManager transactionManager = new RawTransactionManager(web3j,
                    credentials,
                    512512);
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    toAddress,
                    value,
                    "");
            System.out.println("toAddress:" + toAddress);
            EthSendTransaction ethSendTransaction = transactionManager.signAndSend(rawTransaction);
            String transactionHash = ethSendTransaction.getTransactionHash();
            if (ethSendTransaction.hasError()) {
                String message = ethSendTransaction.getError().getMessage();
                System.out.println("transaction failed,info:" + message);
            } else {
                EthGetTransactionReceipt send = web3j.ethGetTransactionReceipt(transactionHash).send();
                if (send != null) {
                    System.out.println("transaction success");
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void signAfterSubmit(String rpcUrl, String fromPk, String toAddress, String amount) {
        try {
            SignAndSubmit signAndSubmit = new SignAndSubmit(rpcUrl);
            // sign transaction
            byte[] signMessage = signAndSubmit.offlineSign(fromPk, toAddress, new BigInteger(amount));
            if (Objects.isNull(signMessage)) {
                return;
            }
            // submit sign message
            signAndSubmit.submitSign(signMessage);
        } catch (Exception e) {
            System.out.println("transaction failed,exception:" + e);
            web3j.shutdown();
        }finally {
            web3j.shutdown();
        }
    }

    public static boolean signAndSubmit(String rpcUrl, String fromPk, String toAddress, String amount) {
        try {
            SignAndSubmit signAndSubmit = new SignAndSubmit(rpcUrl);
            return signAndSubmit.signAndSend(fromPk, toAddress, new BigInteger(amount));
        } catch (Exception e) {
            web3j.shutdown();
            System.out.println("transaction failed,exception:" + e);
            return false;
        } finally {
            web3j.shutdown();
        }
    }
}
