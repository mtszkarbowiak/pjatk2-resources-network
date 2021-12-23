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
    public static final String PassAllocationRequest = "ASK_MASTER_FOR_ALLOC"; // ASK_MASTER_FOR_ALLOC <id> <rsc0:n0> <...>
    public static final String PassAllocationResponseFailNoMaster = "SLAVE_ALLOCED_FAIL_NO_MASTER";
    public static final String PassAllocationResponseFailNoSpace = "SLAVE_ALLOCED_FAIL_NO_SPACE";

    // Slave Alloc Dictation
    public static final String AllocateRequest = "MAKE_ALLOC"; // MAKE_ALLOC <id> <rsc0:n0> <...>
    public static final String AllocateResponse = "ALLOCED_SIR"; // ALLOCED_SIR <rsc0:n0>
}
