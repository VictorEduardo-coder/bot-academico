package src.bot;

import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;

import com.src.model.Disciplina;
import com.src.service.EmailReaderService;
import com.src.service.OptimizerService;

public class BotManager {
    private EmailReaderService emailReaderService;
    private OptimizerService optimizerService;

    public BotManager() {
        this.emailReaderService = new EmailReaderService();
        this.optimizerService = new OptimizerService();
    }

    public void iniciar() {
        // Lógica para iniciar o bot, como verificar emails e otimizar horários
        emailReaderService.verificarNovosPrazos();
        // Supondo que você tenha uma lista de disciplinas cadastradas
        List<Disciplina> disciplinasCadastradas = obterDisciplinasCadastradas();
        PriorityQueue<Disciplina> filaPrioridade = optimizerService.gerarFilaDePrioridade(disciplinasCadastradas);
        optimizerService.alocarBlocosDeEstudo(filaPrioridade);
    }

    private List<Disciplina> obterDisciplinasCadastradas() {
        // Lógica para obter disciplinas cadastradas do banco de dados ou outra fonte
        return new ArrayList<>();
    }
}