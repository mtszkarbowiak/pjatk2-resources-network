package rscnet.communication;

public class NetCommands {
    // Navigation to Master (SH <-> SH)
    public static final String HeadRequest = "SHOW_MASTER";
    public static final String HeadResponseAboutMaster = "ASK_HIM";
    public static final String HeadResponseMeMaster = "I_AM_MASTER";
    public static final String HeadResponseFail = "UNKNOWN_MASTER";

    // Slave Registration (SH <-> MH)
    public static final String RegistrationRequest = "REG_SLAVE"; // REG_SLAVE <id>
    public static final String RegistrationResponseDeny = "REG_SLAVE_DENY";
    public static final String RegistrationResponseSuccess = "REG_SLAVE_OK";

    // Allocation Pass (SH <-> M)
    public static final String NewLineReplacer = ":::";
}
