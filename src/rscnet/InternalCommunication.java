package rscnet;

import rscnet.data.*;

public class InternalCommunication {
    public final InternalPass<AllocationRequest> allocationRequestInternalPass
            = new InternalPass<>(true);

    public final InternalPass<String> allocationResponseInternalPass
            = new InternalPass<>(true);

    public final InternalPass<Boolean> registrationConfirmation
            = new InternalPass<>(false, false);


    public final InternalPass<Boolean> terminationRequestInternalPass
            = new InternalPass<>(true);

    public final InternalPass<String> terminationResponseInternalPass
            = new InternalPass<>(true);

    public final InternalPass<Boolean> collapseNetworkInternalPass
             = new InternalPass<>(false, false);
}