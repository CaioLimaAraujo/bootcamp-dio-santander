package dio.budgeting.domain;

import lombok.Getter;

@Getter
public class Transaction {
    private final TransactionId id;
    private final String description;
    private final long amount;
    private final Category category;

    public Transaction(TransactionId id, String description, long amount, Category category) {
        validate(description, amount, category);
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
    }

    public Transaction(String description, long amount, Category category) {
        this(new TransactionId(), description, amount, category);
    }

    private static void validate(String description, long amount, Category category) {
        if (description == null || description.isBlank()) {
            throw new InvalidTransactionException("A descrição da transação não pode estar vazia.");
        }
        if (amount <= 0) {
            throw new InvalidTransactionException("O valor da transação deve ser maior que zero.");
        }
        if (category == null) {
            throw new InvalidTransactionException("A categoria da transação é obrigatória.");
        }
    }
}