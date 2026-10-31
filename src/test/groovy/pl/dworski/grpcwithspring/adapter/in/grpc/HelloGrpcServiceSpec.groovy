package pl.dworski.grpcwithspring.adapter.in.grpc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.grpc.client.ImportGrpcClients
import org.springframework.test.context.ActiveProfiles
import pl.dworski.grpcwithspring.MongoTestContainerConfiguration
import pl.dworski.grpcwithspring.proto.HelloRequest
import pl.dworski.grpcwithspring.proto.HelloServiceGrpc
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
@ImportGrpcClients(types = HelloServiceGrpc.HelloServiceBlockingStub)
class HelloGrpcServiceSpec extends Specification {

    @Autowired
    HelloServiceGrpc.HelloServiceBlockingStub helloStub

    def "sayHello returns greeting for given name"() {
        when:
        def reply = helloStub.sayHello(HelloRequest.newBuilder().setName(name).build())

        then:
        reply.message == expected

        where:
        name   || expected
        "Luke" || "Hello Luke"
        "Anna" || "Hello Anna"
    }

}
