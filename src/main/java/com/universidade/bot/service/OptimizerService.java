package com.universidade.bot.service;

import com.universidade.bot.model.BlocoEstudo;
import com.universidade.bot.model.Disciplina;
import com.universidade.bot.model.EventoAcademico;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class OptimizerService {

    private static final int MINUTOS_BLOCO_PADRAO = 50;
    private static final int HORAS_DISPONIVEIS_SEMANAIS = 40;

    private static final Map<DayOfWeek, List<LocalTime>> HORARIOS_DISPONIVEIS = new LinkedHashMap<>();

    static {
        HORARIOS_DISPONIVEIS.put(DayOfWeek.MONDAY, Arrays.asList(
                LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(16, 0)
        ));
        HORARIOS_DISPONIVEIS.put(DayOfWeek.TUESDAY, Arrays.asList(
                LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(16, 0)
        ));
        HORARIOS_DISPONIVEIS.put(DayOfWeek.WEDNESDAY, Arrays.asList(
                LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(16, 0)
        ));
        HORARIOS_DISPONIVEIS.put(DayOfWeek.THURSDAY, Arrays.asList(
                LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(16, 0)
        ));
        HORARIOS_DISPONIVEIS.put(DayOfWeek.FRIDAY, Arrays.asList(
                LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(14, 0)
        ));
    }

    public PriorityQueue<Disciplina> gerarFilaDePrioridade(List<Disciplina> disciplinas, List<EventoAcademico> eventos) {
        Map<Long, Double> urgenciaMap = new HashMap<>();
        for (EventoAcademico evento : eventos) {
            urgenciaMap.merge(evento.getDisciplinaId(), evento.urgencia(), Math::max);
        }

        PriorityQueue<Disciplina> fila = new PriorityQueue<>(
                Comparator.comparingDouble((Disciplina d) -> {
                    double prioridadeBase = d.calcularFatorPrioridade();
                    double urgencia = urgenciaMap.getOrDefault(d.getId(), 0.0);
                    return prioridadeBase + (urgencia * 0.3);
                }).reversed()
        );

        fila.addAll(disciplinas);
        return fila;
    }

    public List<BlocoEstudo> alocarBlocosDeEstudo(PriorityQueue<Disciplina> filaPrioridade) {
        List<BlocoEstudo> blocosAlocados = new ArrayList<>();

        Map<DayOfWeek, List<LocalTime>> horariosRestantes = new LinkedHashMap<>();
        for (Map.Entry<DayOfWeek, List<LocalTime>> entry : HORARIOS_DISPONIVEIS.entrySet()) {
            horariosRestantes.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        Map<Long, Integer> horasAlocadasPorDisciplina = new HashMap<>();

        int totalBlocosDisponiveis = horariosRestantes.values().stream()
                .mapToInt(List::size).sum();

        while (!filaPrioridade.isEmpty() && totalBlocosDisponiveis > 0) {
            Disciplina disciplina = filaPrioridade.poll();

            int horasNecessarias = disciplina.getHorasSemanaisNecessarias();
            int horasJaAlocadas = horasAlocadasPorDisciplina.getOrDefault(disciplina.getId(), 0);

            if (horasJaAlocadas >= horasNecessarias) {
                continue;
            }

            int blocosNecessarios = (int) Math.ceil((double) (horasNecessarias - horasJaAlocadas) * 60 / MINUTOS_BLOCO_PADRAO);
            int blocosAlocadosParaDisciplina = 0;

            for (Map.Entry<DayOfWeek, List<LocalTime>> entry : horariosRestantes.entrySet()) {
                if (blocosAlocadosParaDisciplina >= blocosNecessarios) {
                    break;
                }

                List<LocalTime> horarios = entry.getValue();
                if (!horarios.isEmpty()) {
                    LocalTime horario = horarios.remove(0);
                    blocosAlocados.add(new BlocoEstudo(disciplina, entry.getKey(), horario, MINUTOS_BLOCO_PADRAO));
                    blocosAlocadosParaDisciplina++;
                    totalBlocosDisponiveis--;
                }
            }

            horasAlocadasPorDisciplina.merge(disciplina.getId(),
                    blocosAlocadosParaDisciplina * MINUTOS_BLOCO_PADRAO / 60, Integer::sum);
        }

        return blocosAlocados;
    }

    public Map<DayOfWeek, List<BlocoEstudo>> agruparPorDia(List<BlocoEstudo> blocos) {
        return blocos.stream()
                .collect(Collectors.groupingBy(
                        BlocoEstudo::getDia,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public String formatarCronograma(List<BlocoEstudo> blocos) {
        if (blocos.isEmpty()) {
            return "Nenhum bloco de estudo alocado.";
        }

        Map<DayOfWeek, List<BlocoEstudo>> porDia = agruparPorDia(blocos);
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<DayOfWeek, List<BlocoEstudo>> entry : porDia.entrySet()) {
            sb.append(String.format("**%s**\n", formatarDia(entry.getKey())));

            entry.getValue().stream()
                    .sorted(Comparator.comparing(BlocoEstudo::getInicio))
                    .forEach(bloco -> {
                        sb.append(String.format("`%s` → **%s** (%d min)\n",
                                bloco.toHorario(),
                                bloco.getDisciplina().getNome(),
                                bloco.getDuracaoMinutos()));
                    });

            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatarDia(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "Segunda-feira";
            case TUESDAY -> "Terça-feira";
            case WEDNESDAY -> "Quarta-feira";
            case THURSDAY -> "Quinta-feira";
            case FRIDAY -> "Sexta-feira";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }
}
