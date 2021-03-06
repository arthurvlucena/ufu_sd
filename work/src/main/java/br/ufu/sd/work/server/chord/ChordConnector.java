package br.ufu.sd.work.server.chord;

import com.google.protobuf.Empty;
import io.grpc.*;

public class ChordConnector {

    private String ip;
    private int port;
    private int jumpNextPort;
    private long firstNode;
    private long offSetId;
    private int numberOfNodes;

    public ChordConnector(String ip, int port, int jumpNextPort, int numberOfNodes, int numberBitsId) {
        this.ip = ip;
        this.port = port;
        this.jumpNextPort = jumpNextPort;

        this.firstNode = (long) Math.pow(2, numberBitsId) - 1;
        this.offSetId = (long) Math.pow(2, numberBitsId) / numberOfNodes;
        this.numberOfNodes = numberOfNodes;
    }

    public ChordNode connect() throws ChordException {
        ChordNode node = new ChordNode();
        node.setNodeId(numberOfNodes);
        node.setIp(ip);
        node.setPort(port);
        node.setMaxId(firstNode);
        node.setNumberOfNodes(numberOfNodes);
        node.setOffSetId(offSetId);
        node.setFirstNode(true);

        return tryConnectOnRing(node);
    }

    public ChordNode tryConnectOnRing(ChordNode candidateNode) throws ChordException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(candidateNode.getIp(), candidateNode.getPort())
                .usePlaintext().build();

        ChordServiceGrpc.ChordServiceBlockingStub stub = ChordServiceGrpc.newBlockingStub(channel);
        DataNode channelNodeFromNext = null;

        try {
            channelNodeFromNext = stub.heyListen(Empty.newBuilder().build());
        } catch (StatusRuntimeException e) {
            // TODO diferenciar de nenhum serviço rodando na porta, de uma porta já ocupada
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                System.out.println("não tem ngm");
            }

            if (e.getStatus().getCode().equals(Status.Code.ALREADY_EXISTS) || e.getStatus().getCode().equals(Status.Code.UNIMPLEMENTED)) {
                System.out.println("ocupada outro serviço ou outro servidor grpc");
            }
        }

        if (!channel.isShutdown()) {
            channel.shutdownNow();
        }

        if (channelNodeFromNext == null) {
            if (!candidateNode.isFirstNode()) {
                ManagedChannel channelNext = ManagedChannelBuilder.forAddress(candidateNode.getIpNext(), candidateNode.getPortNext())
                        .usePlaintext().build();

                ChordServiceGrpc.ChordServiceBlockingStub stubNext = ChordServiceGrpc.newBlockingStub(channelNext);

                DataNode previous = DataNode.newBuilder()
                        .setIp(candidateNode.getIp())
                        .setPort(candidateNode.getPort())
                        .build();

                stubNext.setPrevious(previous);

                if (!channelNext.isShutdown()) {
                    channelNext.shutdownNow();
                }
            }

            if (candidateNode.isLastNode()) {
                ManagedChannel channelNext = ManagedChannelBuilder.forAddress(candidateNode.getIpNext(), candidateNode.getPortNext())
                        .usePlaintext().build();

                ChordServiceGrpc.ChordServiceBlockingStub stubNext = ChordServiceGrpc.newBlockingStub(channelNext);

                DataNode lastNode = DataNode.newBuilder()
                        .setIp(candidateNode.getIp())
                        .setPort(candidateNode.getPort())
                        .build();

                DataNode first = stubNext.setFirstAndLastNode(lastNode);

                candidateNode.setPrevious(first);

                if (!channelNext.isShutdown()) {
                    channelNext.shutdownNow();
                }
            }

            System.out.println("EU SOU ----\n" + candidateNode.toString());

            return candidateNode;
        } else {
            boolean lastNode = candidateNode.getNodeId() - 1 == 1l;

            ChordNode newCandidateNode = new ChordNode();
            newCandidateNode.setNodeId(candidateNode.getNodeId() - 1);
            newCandidateNode.setIp(ip);
            newCandidateNode.setPort(candidateNode.getPort() + jumpNextPort);// TODO diferenciar de nenhum serviço rodando na porta, de uma porta já ocupada, se porta estiver ocupada só incrementar a porta
            newCandidateNode.setMaxId(candidateNode.getMaxId());
            newCandidateNode.setNumberOfNodes(numberOfNodes);
            newCandidateNode.setOffSetId(offSetId);
            newCandidateNode.setMaxId(firstNode);
            newCandidateNode.setNext(channelNodeFromNext);
            newCandidateNode.setFirstNode(false);
            newCandidateNode.setLastNode(lastNode);

            if (newCandidateNode.getNodeId() > 0 && !channelNodeFromNext.getLastNode()) {
                return tryConnectOnRing(newCandidateNode);
            } else {
                throw new ChordException("Chord Ring is full!!");
            }
        }
    }
}
