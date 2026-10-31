package pl.dworski.grpcwithspring.adapter.in.grpc;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import pl.dworski.grpcwithspring.proto.HelloRequest;
import pl.dworski.grpcwithspring.proto.HelloResponse;
import pl.dworski.grpcwithspring.proto.HelloServiceGrpc;

@Service
class HelloGrpcService extends HelloServiceGrpc.HelloServiceImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        var response = HelloResponse.newBuilder()
                .setMessage("Hello " + request.getName())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
