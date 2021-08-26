package com.hou.hacker.question;


import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import javax.net.ssl.HttpsURLConnection;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

class Transactions {
    private Integer page;
    @SerializedName("per_page")
    private Integer perPage;
    private Integer total;
    @SerializedName("total_pages")
    private Integer totalPages = 0;
    private List<Transaction> data;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public List<Transaction> getData() {
        return data;
    }

    public void setData(List<Transaction> data) {
        this.data = data;
    }
}

class Transaction {
    private Integer id;
    private Integer userId;
    private String userName;
    private String txnType;
    private String amount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txtType) {
        this.txnType = txtType;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}

enum TransactionType {
    CREDIT,
    DEBIT
}

public class HackerRankerTransactions {

    private final static String TRANSACTIONS_URL = "https://jsonmock.hackerrank.com/api/transactions/search?page=%s";
    private ScriptEngine engine;

    public void testRun(boolean isGson) {
        if (!isGson) {
            this.engine = new ScriptEngineManager().getEngineByName("javascript");
        }
        List<Transaction> transactionList = getAllTransaction(isGson);
        Map<Integer, BigDecimal> userBalanceMap = getUserBalance(transactionList);
        System.out.println("All user balance:" + userBalanceMap);
        userBalanceMap.entrySet().stream().max(Map.Entry.comparingByValue()).ifPresent(e -> System.out.println("Max balance user:" + e.getKey()));
    }

    private Map<Integer, BigDecimal> getUserBalance(List<Transaction> transactionList) {
        Map<Integer, List<Transaction>> userTransactionsMap = transactionList.stream().collect(Collectors.groupingBy(Transaction::getUserId));
        Map<Integer, BigDecimal> userBalanceMap = new HashMap<>();
        userTransactionsMap.forEach((userId, transactions) -> {
            BigDecimal debit = transactions.stream().filter(t -> TransactionType.DEBIT.name().equalsIgnoreCase(t.getTxnType())).map(Transaction::getAmount).map(this::amountConverter).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credit = transactions.stream().filter(t -> TransactionType.CREDIT.name().equalsIgnoreCase(t.getTxnType())).map(Transaction::getAmount).map(this::amountConverter).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal balance = debit.subtract(credit);
            userBalanceMap.put(userId, balance);
        });
        return userBalanceMap;
    }

    private BigDecimal amountConverter(String amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
        try {
            return new BigDecimal(nf.parse(amount).toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    private List<Transaction> getAllTransaction(boolean isGson) {
        Integer totalPages = getTotalPages(isGson);
        List<Transaction> transactionList = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            getTransactions(isGson, i).ifPresent(transactions -> transactionList.addAll(transactions.getData()));
        }
        return transactionList;
    }

    private Integer getTotalPages(boolean isGson) {
        return getTransactions(isGson, 1).orElse(new Transactions()).getTotalPages();
    }

    private Optional<Transactions> getTransactions(boolean isGson, Integer page) {
        BufferedReader buff = null;
        try {

            URL url = new URL(String.format(TRANSACTIONS_URL, page));
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setReadTimeout(60 * 1000);
            buff = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String input;
            StringBuilder stringBuilder = new StringBuilder();
            while ((input = buff.readLine()) != null) {
                stringBuilder.append(input);
            }
            return Optional.ofNullable(isGson ? new GsonBuilder().create().fromJson(stringBuilder.toString(), Transactions.class) : parseJson(stringBuilder.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Optional.empty();
    }

    private Transactions parseJson(String json) {
        String script = "Java.asJSONCompatible(" + json + ")";
        Object result = null;
        try {
            result = this.engine.eval(script);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        Map contentsMap = (Map) result;
        Transactions transactions = new Transactions();
        Optional.ofNullable(contentsMap).ifPresent(contents -> contents.forEach((t, u) -> {
            transactions.setTotalPages((Integer) contentsMap.get("total_pages"));
            List<Map> data = (List<Map>) contentsMap.get("data");
            transactions.setData(dataToTransaction(data));
        }));
        return transactions;
    }

    private List<Transaction> dataToTransaction(List<Map> data) {
        List<Transaction> transactionList = new ArrayList<>();
        data.forEach(d -> {
            Transaction transaction = new Transaction();
            transaction.setUserId((Integer) d.get("userId"));
            transaction.setTxnType((String) d.get("txnType"));
            transaction.setAmount((String) d.get("amount"));
            transactionList.add(transaction);
        });
        return transactionList;
    }

}
