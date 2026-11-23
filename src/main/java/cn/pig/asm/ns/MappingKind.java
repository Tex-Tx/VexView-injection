package cn.pig.asm.ns;

public enum MappingKind {
    MCP(null),
    SRG("/mappings/mcp-srg.srg"),
    NOTCH("/mappings/mcp-notch.srg");

    private final String resource;

    MappingKind(String resource) {
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}
