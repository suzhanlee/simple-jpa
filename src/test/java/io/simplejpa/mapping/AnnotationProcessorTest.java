package io.simplejpa.mapping;

import io.simplejpa.annotation.Column;
import io.simplejpa.annotation.Entity;
import io.simplejpa.annotation.Id;
import io.simplejpa.metadata.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnnotationProcessorTest {
    private AnnotationProcessor annotationProcessor;

    @BeforeEach
    void setUp() {
        annotationProcessor = new AnnotationProcessor();
    }

    @Test
    @DisplayName("entity를 메타데이터로 변환할 수 있다.")
    void processEntity() {
        // when
        EntityMetadata result = annotationProcessor.processEntity(Member.class);

        // then
        assertThat(result.getEntityName()).isEqualTo("Member");
        assertThat(result.getIdentifierMetadata().getFieldName()).isEqualTo("id");
        assertThat(result.getAttributeMetadata("name").getFieldName()).isEqualTo("name");
        assertThat(result.getAttributeMetadata("email").getFieldName()).isEqualTo("email");
    }

    @Test
    @DisplayName("Entity에 @Entity가 없으면 예외가 발생한다.")
    void validateEntityAnnotationExists() {
        // when // then
        assertThatThrownBy(() -> annotationProcessor.processEntity(String.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Entity에 기본 생성자가 없으면 예외가 발생한다.")
    void validateEntityDefaultConstructorExists() {
        // when // then
        assertThatThrownBy(() -> annotationProcessor.processEntity(NoDefaultConstructor.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Entity에 @Id가 없으면 예외가 발생한다.")
    void validateEntityIdAnnotationExists() {
        // when // then
        assertThatThrownBy(() -> annotationProcessor.processEntity(NoIdAnnotation.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Entity
    static class Member {
        @Id
        private Long id;

        @Column(name = "name", nullable = false)
        private String name;

        @Column(name = "email", nullable = false)
        private String email;

        public Member() {
        }
    }

    @Entity
    static class NoDefaultConstructor {
        @Id
        private Long id;

        public NoDefaultConstructor(Long id) {
            this.id = id;
        }
    }

    @Entity
    static class NoIdAnnotation {
        private Long id;
        private String name;
        private String email;

        public NoIdAnnotation() {
        }
    }

}