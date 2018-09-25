package br.ufu.sd.work.util.commands;

import br.ufu.sd.work.model.Dictionary;
import br.ufu.sd.work.server.OutputStreamCommand;
import br.ufu.sd.work.util.commands.api.ICommand;

import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class Select implements ICommand {
    String response;
    Integer count = 0;

    @Override
    public void run(OutputStreamCommand osc, Dictionary dictionary) {
        String[] args = osc.getMessageCommand().getArgs();
        System.out.println("executando commando de select com os argumentos" + args);
        String formattedInsert = String.join(",", args);

        for (Long key : dictionary.getData().keySet()) {
            if (Arrays.equals(formattedInsert.getBytes(), dictionary.getData().get(key))) {
                try {
                    String responseValue = new String(dictionary.getData().get(key), "UTF-8");
                    response = "KEY: " + key + " VALUE: " + responseValue;
                } catch (UnsupportedEncodingException e) {
                    System.out.println(e);
                }

                count = count + 1;
                osc.getMessageCommand().setResponse(response);
                System.out.println("select realizado");
            }
        }

        if(count == 0){
            osc.getMessageCommand().setResponse("Não foi encontrado resposta.");
        }
    }
}