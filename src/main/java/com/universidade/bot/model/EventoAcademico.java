package com.universidade.bot.model;

import java.time.LocalDateTime;

public class EventoAcademico {
    private Long id;
    private Long disciplinaId;
    private String tipo;
    private LocalDateTime dataEvento;
    private String descricao;

    public EventoAcademico() {}

    public EventoAcademico(Long disciplinaId, String tipo, LocalDateTime dataEvento, String descricao) {
        this.disciplinaId = disciplinaId;
        this.tipo = tipo;
        this.dataEvento = dataEvento;
        this.descricao = descricao;
    }

    public long diasAteEvento() {
        return java.time.Duration.between(LocalDateTime.now(), dataEvento).toDays();
    }

    public double urgencia() {
        long dias = diasAteEvento();
        if (dias <= 0) return 10.0;
        if (dias <= 3) return 9.0;
        if (dias <= 7) return 7.0;
        if (dias <= 14) return 5.0;
        return 3.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDisciplinaId() { return disciplinaId; }
    public void setDisciplinaId(Long disciplinaId) { this.disciplinaId = disciplinaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDateTime getDataEvento() { return dataEvento; }
    public void setDataEvento(LocalDateTime dataEvento) { this.dataEvento = dataEvento; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
