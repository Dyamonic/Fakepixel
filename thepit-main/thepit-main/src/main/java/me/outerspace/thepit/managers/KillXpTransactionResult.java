package me.outerspace.thepit.managers;

public final class KillXpTransactionResult {
    public enum Status { SUCCESS, CONVERTED_TO_GOLD, DUPLICATE, INVALID, SAVE_FAILURE }

    private final Status status;
    private final KillXpResult calculation;
    private final int grantedXp;
    private final int convertedGold;

    public KillXpTransactionResult(Status status, KillXpResult calculation, int grantedXp, int convertedGold) {
        this.status = status;
        this.calculation = calculation;
        this.grantedXp = grantedXp;
        this.convertedGold = convertedGold;
    }

    public Status getStatus() { return status; }
    public KillXpResult getCalculation() { return calculation; }
    public int getGrantedXp() { return grantedXp; }
    public int getConvertedGold() { return convertedGold; }
    public boolean succeeded() { return status == Status.SUCCESS || status == Status.CONVERTED_TO_GOLD; }
}
