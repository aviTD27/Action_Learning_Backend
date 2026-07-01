package fr.epita.service;

import fr.epita.dto.Response.ComplianceReportResponse;
import fr.epita.model.SubmissionRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Service
public class AiServiceClient {

    private final RestClient restClient;

    public AiServiceClient(
            RestClient.Builder builder,
            @Value("${ai.service.url:http://localhost:8001}") String aiServiceUrl) {
        this.restClient = builder.baseUrl(aiServiceUrl).build();
    }

    public ComplianceReportResponse check(MultipartFile file, SubmissionRules rules) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        byte[] bytes = file.getBytes();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() { return filename; }
        });

        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/check");
        if (rules != null) {
            if (rules.getMinWordCount() != null && rules.getMinWordCount() > 0)
                uri.queryParam("min_word_count", rules.getMinWordCount());
            if (rules.getMaxWordCount() != null && rules.getMaxWordCount() > 0)
                uri.queryParam("max_word_count", rules.getMaxWordCount());
            if (rules.getNamingPattern() != null && !rules.getNamingPattern().isBlank())
                uri.queryParam("naming_pattern", rules.getNamingPattern());
            if (rules.getRequiredHeadings() != null && !rules.getRequiredHeadings().isBlank())
                uri.queryParam("required_headings", rules.getRequiredHeadings());
            if (rules.getAllowedFileTypes() != null && !rules.getAllowedFileTypes().isBlank())
                uri.queryParam("allowed_file_types", rules.getAllowedFileTypes());
        }

        return restClient.post()
                .uri(uri.toUriString())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(ComplianceReportResponse.class);
    }
}
