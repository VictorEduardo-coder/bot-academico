package com.universidade.bot.database;

import com.universidade.bot.model.Disciplina;
import com.universidade.bot.model.EventoAcademico;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private final boolean usingPostgres;

    private DatabaseManager() {
        this.usingPostgres = System.getenv("DATABASE_URL") != null;
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void inicializar() {
        try {
            if (usingPostgres) {
                String dbUrl = System.getenv("DATABASE_URL");
                connection = DriverManager.getConnection(dbUrl);
                System.out.println("Conectado ao PostgreSQL.");
            } else {
                connection = DriverManager.getConnection("jdbc:h2:./bot_academico;AUTO_SERVER=TRUE");
                System.out.println("Conectado ao H2 (local).");
            }
            criarTabelas();
            System.out.println("Banco de dados inicializado com sucesso.");
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar banco: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void criarTabelas() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            if (usingPostgres) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS disciplinas (
                        id SERIAL PRIMARY KEY,
                        nome VARCHAR(100) NOT NULL,
                        peso INT NOT NULL,
                        dificuldade INT NOT NULL,
                        horas_semanais INT NOT NULL,
                        user_id VARCHAR(50) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS eventos (
                        id SERIAL PRIMARY KEY,
                        disciplina_id BIGINT NOT NULL,
                        tipo VARCHAR(50) NOT NULL,
                        data_evento TIMESTAMP NOT NULL,
                        descricao VARCHAR(500),
                        notificado BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id) ON DELETE CASCADE
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS email_config (
                        user_id VARCHAR(50) PRIMARY KEY,
                        email VARCHAR(100) NOT NULL,
                        senha VARCHAR(255) NOT NULL,
                        host VARCHAR(50) DEFAULT 'imap.gmail.com',
                        porta INT DEFAULT 993,
                        ativo BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS notificacoes (
                        id SERIAL PRIMARY KEY,
                        user_id VARCHAR(50) NOT NULL,
                        canal_id VARCHAR(50) NOT NULL,
                        dias_antes INT DEFAULT 3,
                        hora_notificacao VARCHAR(5) DEFAULT '09:00',
                        ativo BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            } else {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS disciplinas (
                        id IDENTITY PRIMARY KEY,
                        nome VARCHAR(100) NOT NULL,
                        peso INT NOT NULL,
                        dificuldade INT NOT NULL,
                        horas_semanais INT NOT NULL,
                        user_id VARCHAR(50) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS eventos (
                        id IDENTITY PRIMARY KEY,
                        disciplina_id BIGINT NOT NULL,
                        tipo VARCHAR(50) NOT NULL,
                        data_evento TIMESTAMP NOT NULL,
                        descricao VARCHAR(500),
                        notificado BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id) ON DELETE CASCADE
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS email_config (
                        user_id VARCHAR(50) PRIMARY KEY,
                        email VARCHAR(100) NOT NULL,
                        senha VARCHAR(255) NOT NULL,
                        host VARCHAR(50) DEFAULT 'imap.gmail.com',
                        porta INT DEFAULT 993,
                        ativo BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS notificacoes (
                        id IDENTITY PRIMARY KEY,
                        user_id VARCHAR(50) NOT NULL,
                        canal_id VARCHAR(50) NOT NULL,
                        dias_antes INT DEFAULT 3,
                        hora_notificacao VARCHAR(5) DEFAULT '09:00',
                        ativo BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            }
        }
    }

    // ==================== EMAIL CONFIG ====================

    public void salvarEmailConfig(String userId, String email, String senha) {
        String sql = usingPostgres ?
            """
                INSERT INTO email_config (user_id, email, senha, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id) DO UPDATE SET email = ?, senha = ?, updated_at = CURRENT_TIMESTAMP
            """ :
            """
                MERGE INTO email_config (user_id, email, senha, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, email);
            ps.setString(3, senha);
            if (usingPostgres) {
                ps.setString(4, email);
                ps.setString(5, senha);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao salvar config email: " + e.getMessage());
        }
    }

    public String[] obterEmailConfig(String userId) {
        String sql = "SELECT email, senha FROM email_config WHERE user_id = ? AND ativo = TRUE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[]{rs.getString("email"), rs.getString("senha")};
            }
        } catch (SQLException e) {
            System.err.println("Erro ao obter config email: " + e.getMessage());
        }
        return null;
    }

    public List<String[]> listarTodasEmailConfigs() {
        List<String[]> configs = new ArrayList<>();
        String sql = "SELECT user_id, email, senha FROM email_config WHERE ativo = TRUE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                configs.add(new String[]{
                        rs.getString("user_id"),
                        rs.getString("email"),
                        rs.getString("senha")
                });
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar configs email: " + e.getMessage());
        }
        return configs;
    }

    public boolean removerEmailConfig(String userId) {
        String sql = "UPDATE email_config SET ativo = FALSE WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao remover config email: " + e.getMessage());
            return false;
        }
    }

    // ==================== DISCIPLINAS ====================

    public void adicionarDisciplina(Disciplina disciplina) {
        String sql = "INSERT INTO disciplinas (nome, peso, dificuldade, horas_semanais, user_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, disciplina.getNome());
            ps.setInt(2, disciplina.getPeso());
            ps.setInt(3, disciplina.getDificuldade());
            ps.setInt(4, disciplina.getHorasSemanaisNecessarias());
            ps.setString(5, disciplina.getUserId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                disciplina.setId(rs.getLong(1));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar disciplina: " + e.getMessage());
        }
    }

    public boolean removerDisciplina(String nome, String userId) {
        String sql = "DELETE FROM disciplinas WHERE nome = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao remover disciplina: " + e.getMessage());
            return false;
        }
    }

    public List<Disciplina> listarTodas() {
        List<Disciplina> disciplinas = new ArrayList<>();
        String sql = "SELECT * FROM disciplinas ORDER BY nome";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                disciplinas.add(mapearDisciplina(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar disciplinas: " + e.getMessage());
        }
        return disciplinas;
    }

    public List<Disciplina> listarPorUsuario(String userId) {
        List<Disciplina> disciplinas = new ArrayList<>();
        String sql = "SELECT * FROM disciplinas WHERE user_id = ? ORDER BY nome";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                disciplinas.add(mapearDisciplina(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar disciplinas: " + e.getMessage());
        }
        return disciplinas;
    }

    // ==================== EVENTOS ====================

    public void adicionarEvento(EventoAcademico evento) {
        String sql = "INSERT INTO eventos (disciplina_id, tipo, data_evento, descricao) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, evento.getDisciplinaId());
            ps.setString(2, evento.getTipo());
            ps.setTimestamp(3, Timestamp.valueOf(evento.getDataEvento()));
            ps.setString(4, evento.getDescricao());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                evento.setId(rs.getLong(1));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar evento: " + e.getMessage());
        }
    }

    public boolean eventoDuplicado(Long disciplinaId, String tipo, LocalDateTime dataEvento) {
        String sql = "SELECT COUNT(*) FROM eventos WHERE disciplina_id = ? AND tipo = ? AND data_evento = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, disciplinaId);
            ps.setString(2, tipo);
            ps.setTimestamp(3, Timestamp.valueOf(dataEvento));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao verificar duplicata: " + e.getMessage());
        }
        return false;
    }

    public List<EventoAcademico> listarEventosPorDisciplina(Long disciplinaId) {
        List<EventoAcademico> eventos = new ArrayList<>();
        String sql = "SELECT * FROM eventos WHERE disciplina_id = ? AND data_evento > ? ORDER BY data_evento";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, disciplinaId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                eventos.add(mapearEvento(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar eventos: " + e.getMessage());
        }
        return eventos;
    }

    public List<EventoAcademico> listarTodosEventos() {
        List<EventoAcademico> eventos = new ArrayList<>();
        String sql = "SELECT * FROM eventos WHERE data_evento > ? ORDER BY data_evento";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                eventos.add(mapearEvento(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar eventos: " + e.getMessage());
        }
        return eventos;
    }

    public List<EventoAcademico> listarEventosProximos(int dias) {
        List<EventoAcademico> eventos = new ArrayList<>();
        String sql = "SELECT * FROM eventos WHERE data_evento > ? AND data_evento <= ? AND notificado = FALSE ORDER BY data_evento";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().plusDays(dias)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                eventos.add(mapearEvento(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar eventos proximos: " + e.getMessage());
        }
        return eventos;
    }

    public void marcarEventoNotificado(Long eventoId) {
        String sql = "UPDATE eventos SET notificado = TRUE WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, eventoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao marcar notificacao: " + e.getMessage());
        }
    }

    // ==================== NOTIFICACOES ====================

    public void salvarNotificacao(String userId, String canalId, int diasAntes, String hora) {
        String sql = usingPostgres ?
            """
                INSERT INTO notificacoes (user_id, canal_id, dias_antes, hora_notificacao)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET canal_id = ?, dias_antes = ?, hora_notificacao = ?
            """ :
            """
                MERGE INTO notificacoes (user_id, canal_id, dias_antes, hora_notificacao)
                VALUES (?, ?, ?, ?)
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, canalId);
            ps.setInt(3, diasAntes);
            ps.setString(4, hora);
            if (usingPostgres) {
                ps.setString(5, canalId);
                ps.setInt(6, diasAntes);
                ps.setString(7, hora);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao salvar notificacao: " + e.getMessage());
        }
    }

    public List<String[]> listarNotificacoesAtivas() {
        List<String[]> notificacoes = new ArrayList<>();
        String sql = "SELECT user_id, canal_id, dias_antes, hora_notificacao FROM notificacoes WHERE ativo = TRUE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                notificacoes.add(new String[]{
                        rs.getString("user_id"),
                        rs.getString("canal_id"),
                        String.valueOf(rs.getInt("dias_antes")),
                        rs.getString("hora_notificacao")
                });
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar notificacoes: " + e.getMessage());
        }
        return notificacoes;
    }

    // ==================== MAPEAMENTO ====================

    private Disciplina mapearDisciplina(ResultSet rs) throws SQLException {
        return new Disciplina(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getInt("peso"),
                rs.getInt("dificuldade"),
                rs.getInt("horas_semanais"),
                rs.getString("user_id")
        );
    }

    private EventoAcademico mapearEvento(ResultSet rs) throws SQLException {
        EventoAcademico evento = new EventoAcademico();
        evento.setId(rs.getLong("id"));
        evento.setDisciplinaId(rs.getLong("disciplina_id"));
        evento.setTipo(rs.getString("tipo"));
        evento.setDataEvento(rs.getTimestamp("data_evento").toLocalDateTime());
        evento.setDescricao(rs.getString("descricao"));
        return evento;
    }

    public void fechar() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexao com banco fechada.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar banco: " + e.getMessage());
        }
    }
}
