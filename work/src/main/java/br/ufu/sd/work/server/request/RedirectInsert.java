package br.ufu.sd.work.server.request;

import br.ufu.sd.work.CrudServiceGrpc;
import br.ufu.sd.work.InsertRequest;
import br.ufu.sd.work.Response;
import br.ufu.sd.work.model.ResponseCommand;
import br.ufu.sd.work.server.chord.ChordNode;
import br.ufu.sd.work.server.chord.ChordNodeUtils;
import io.grpc.ManagedChannel;

public class RedirectInsert implements Runnable {

    private ChordNode node;
    private ResponseCommand responseCommand;

    public RedirectInsert(ChordNode node, ResponseCommand responseCommand) {
        this.node = node;
        this.responseCommand = responseCommand;
    }

    @Override
    public void run() {
        ManagedChannel channel = ChordNodeUtils.getPossibleResponsibleChannel(node, responseCommand.getCommand().getIdRequest());
        CrudServiceGrpc.CrudServiceBlockingStub stub = CrudServiceGrpc.newBlockingStub(channel);
        Response response = stub.insert((InsertRequest) responseCommand.getCommand().getRequest());
        responseCommand.getStreamObserver().onNext(response);
        responseCommand.getStreamObserver().onCompleted();
    }
}
