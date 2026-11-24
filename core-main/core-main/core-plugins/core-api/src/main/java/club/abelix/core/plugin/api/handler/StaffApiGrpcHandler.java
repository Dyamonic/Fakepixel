package club.abelix.core.plugin.api.handler;

import club.abelix.core.api.proto.v1.GetOnlineStaffRequest;
import club.abelix.core.api.proto.v1.GetOnlineStaffResponse;
import club.abelix.core.api.proto.v1.PunishmentStatsRequest;
import club.abelix.core.api.proto.v1.StaffApiGrpc;
import club.abelix.core.api.proto.v1.StaffStatsResponse;
import club.abelix.core.plugin.api.service.StaffApiService;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class StaffApiGrpcHandler extends StaffApiGrpc.StaffApiImplBase {

    StaffApiService staffApiService;

    @Override
    public void getOnlineStaff(GetOnlineStaffRequest request, StreamObserver<GetOnlineStaffResponse> responseObserver) {
        val response = staffApiService.getOnlineStaff();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void staffStats(PunishmentStatsRequest request, StreamObserver<StaffStatsResponse> responseObserver) {
        staffApiService.buildStats().thenAccept(res -> {
            responseObserver.onNext(StaffStatsResponse.newBuilder().setPunishmentStats(res).build());
            responseObserver.onCompleted();
        });
    }
}
