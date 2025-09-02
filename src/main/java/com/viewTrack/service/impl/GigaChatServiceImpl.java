package com.viewTrack.service.impl;

import chat.giga.client.GigaChatClient;
import chat.giga.model.ModelName;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.service.GigaChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GigaChatServiceImpl implements GigaChatService {

    private final GigaChatClient gigaChatClient;

    @Override
    public String generateMovieReview(Movie movie, List<Review> reviews) {
        try {
            String prompt = buildReviewPrompt(movie, reviews);

            ChatMessage message = ChatMessage.builder()
                    .role(ChatMessage.Role.USER)
                    .content(prompt)
                    .build();

            CompletionRequest request = CompletionRequest.builder()
                    .model(ModelName.GIGA_CHAT_MAX)
                    .messages(List.of(message))
                    .build();

            CompletionResponse response = gigaChatClient.completions(request);
            
            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content();
            } else {
                return "Не удалось сгенерировать рецензию";
            }

        } catch (Exception e) {
            return "Произошла ошибка при генерации рецензии: " + e.getMessage();
        }
    }

    private String buildReviewPrompt(Movie movie, List<Review> reviews) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - профессиональный кинокритик. Напиши краткую рецензию на фильм на основе отзывов зрителей. ");
        prompt.append("Важно: НЕ раскрывай сюжет и спойлеры! Фокусируйся на общих впечатлениях, атмосфере, актерской игре, режиссуре.\n\n");
        
        prompt.append("Информация о фильме:\n");
        prompt.append("- Название: ").append(movie.getTitle()).append("\n");
        if (movie.getReleaseDate() != null) {
            prompt.append("- Год выпуска: ").append(movie.getReleaseDate().getYear()).append("\n");
        }
        if (movie.getDurationMin() > 0) {
            prompt.append("- Длительность: ").append(movie.getDurationMin()).append(" мин\n");
        }
        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            prompt.append("- Жанры: ").append(movie.getGenres().stream()
                    .map(Genre::getGenreName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Не указан")).append("\n");
        }
        
        prompt.append("\nОтзывы зрителей:\n");
        for (Review review : reviews) {
            prompt.append("- Оценка ").append(review.getRating()).append("/10: ")
                    .append(review.getContent()).append("\n");
        }
        
        prompt.append("\nНапиши рецензию объемом 150-200 слов, избегая спойлеров. ");
        prompt.append("Используй информацию из отзывов для анализа качества фильма.");
        
        return prompt.toString();
    }
}
