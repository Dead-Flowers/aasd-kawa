package pl.smartbin;

public enum AgentType {
    BIN,
    BEACON,
    SUPERVISOR,
    GARBAGE_COLLECTOR;

    public String getCode() {
        return name().toLowerCase();
    }
}
