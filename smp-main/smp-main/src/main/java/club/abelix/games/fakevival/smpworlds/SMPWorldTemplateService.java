package club.abelix.games.fakevival.smpworlds;

import java.sql.SQLException;

public final class SMPWorldTemplateService {
    private final SMPWorldRepository repository;

    public SMPWorldTemplateService(SMPWorldRepository repository) {
        this.repository = repository;
    }

    public void cloneTemplate(String templateId, String newWorldId) throws SQLException {
        repository.cloneWorld(templateId, newWorldId);
    }
}
