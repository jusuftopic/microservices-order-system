package com.example.investigationservice.service.knowledge.ingestion;

import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads and validates the version-controlled investigation knowledge corpus.
 */
@Component
public class KnowledgeCorpusLoader {

    private static final String FRONT_MATTER_DELIMITER = "---";

    private final ResourcePatternResolver resourceResolver;

    public KnowledgeCorpusLoader(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public List<InvestigationKnowledgeDocument> load(String corpusVersion) {
        validateCorpusVersion(corpusVersion);
        String location = "classpath*:rag/" + corpusVersion + "/*.md";
        try {
            return Arrays.stream(resourceResolver.getResources(location))
                    .filter(resource -> !"README.md".equals(resource.getFilename()))
                    .sorted(Comparator.comparing(this::filename))
                    .map(resource -> parse(resource, corpusVersion))
                    .filter(document -> Boolean.TRUE.equals(
                            document.metadata().get("effective")
                    ))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load knowledge corpus " + corpusVersion,
                    exception
            );
        }
    }

    private InvestigationKnowledgeDocument parse(
            Resource resource,
            String corpusVersion
    ) {
        try {
            String source = resource.getContentAsString(StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");
            if (!source.startsWith(FRONT_MATTER_DELIMITER + "\n")) {
                throw invalid(resource, "missing metadata header");
            }
            int metadataEnd = source.indexOf(
                    "\n" + FRONT_MATTER_DELIMITER + "\n",
                    FRONT_MATTER_DELIMITER.length() + 1
            );
            if (metadataEnd < 0) {
                throw invalid(resource, "metadata header is not closed");
            }

            String metadataSource = source.substring(4, metadataEnd);
            String content = source.substring(metadataEnd + 5).strip();
            Map<String, Object> metadata = parseMetadata(metadataSource, resource);
            String sourceId = requiredString(metadata, "id", resource);
            Object documentVersion = required(metadata, "version", resource);
            requiredString(metadata, "title", resource);
            requiredString(metadata, "documentType", resource);
            requiredList(metadata, "services", resource);
            requiredList(metadata, "statuses", resource);
            requiredList(metadata, "reasonCodes", resource);
            requiredList(metadata, "decisionCodes", resource);
            Object effective = required(metadata, "effective", resource);
            if (!(effective instanceof Boolean)) {
                throw invalid(resource, "metadata must be boolean: effective");
            }
            if (content.isBlank()) {
                throw invalid(resource, "document content is empty");
            }

            metadata.put("corpusVersion", corpusVersion);
            metadata.put("documentId", sourceId);
            metadata.put("documentVersion", documentVersion);
            metadata.put("contentHash", sha256(source));
            metadata.put("source", filename(resource));

            String vectorDocumentId = UUID.nameUUIDFromBytes(
                    (corpusVersion + ":" + sourceId)
                            .getBytes(StandardCharsets.UTF_8)
            ).toString();
            return new InvestigationKnowledgeDocument(
                    vectorDocumentId,
                    content,
                    Map.copyOf(metadata)
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read knowledge document " + filename(resource),
                    exception
            );
        }
    }

    private Map<String, Object> parseMetadata(
            String source,
            Resource resource
    ) {
        Map<String, Object> metadata = new HashMap<>();
        for (String line : source.lines().toList()) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw invalid(resource, "invalid metadata line: " + line);
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (metadata.putIfAbsent(key, parseValue(value)) != null) {
                throw invalid(resource, "duplicate metadata key: " + key);
            }
        }
        return metadata;
    }

    private Object parseValue(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            String values = value.substring(1, value.length() - 1).trim();
            if (values.isEmpty()) {
                return List.of();
            }
            return Arrays.stream(values.split(","))
                    .map(String::trim)
                    .toList();
        }
        if ("true".equalsIgnoreCase(value)
                || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private Object required(
            Map<String, Object> metadata,
            String key,
            Resource resource
    ) {
        Object value = metadata.get(key);
        if (value == null) {
            throw invalid(resource, "missing metadata: " + key);
        }
        return value;
    }

    private String requiredString(
            Map<String, Object> metadata,
            String key,
            Resource resource
    ) {
        Object value = required(metadata, key, resource);
        if (!(value instanceof String text) || text.isBlank()) {
            throw invalid(resource, "metadata must be text: " + key);
        }
        return text;
    }

    private void requiredList(
            Map<String, Object> metadata,
            String key,
            Resource resource
    ) {
        if (!(required(metadata, key, resource) instanceof List<?>)) {
            throw invalid(resource, "metadata must be a list: " + key);
        }
    }

    private String sha256(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validateCorpusVersion(String corpusVersion) {
        if (corpusVersion == null
                || !corpusVersion.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid knowledge corpus version");
        }
    }

    private IllegalArgumentException invalid(Resource resource, String reason) {
        return new IllegalArgumentException(
                "Invalid knowledge document " + filename(resource) + ": " + reason
        );
    }

    private String filename(Resource resource) {
        return resource.getFilename() == null ? "unknown" : resource.getFilename();
    }
}
