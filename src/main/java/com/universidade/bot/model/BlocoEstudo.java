package com.universidade.bot.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class BlocoEstudo {
    private Disciplina disciplina;
    private DayOfWeek dia;
    private LocalTime inicio;
    private LocalTime fim;
    private int duracaoMinutos;

    public BlocoEstudo(Disciplina disciplina, DayOfWeek dia, LocalTime inicio, int duracaoMinutos) {
        this.disciplina = disciplina;
        this.dia = dia;
        this.inicio = inicio;
        this.fim = inicio.plusMinutes(duracaoMinutos);
        this.duracaoMinutos = duracaoMinutos;
    }

    public Disciplina getDisciplina() { return disciplina; }
    public DayOfWeek getDia() { return dia; }
    public LocalTime getInicio() { return inicio; }
    public LocalTime getFim() { return fim; }
    public int getDuracaoMinutos() { return duracaoMinutos; }

    public String toHorario() {
        return String.format("%s %s-%s", dia, inicio, fim);
    }
}
