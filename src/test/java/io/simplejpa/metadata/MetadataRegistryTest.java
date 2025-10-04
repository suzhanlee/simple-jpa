package io.simplejpa.metadata;

import io.simplejpa.annotation.Column;
import io.simplejpa.annotation.Entity;
import io.simplejpa.annotation.Id;
import io.simplejpa.mapping.AnnotationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataRegistryTest {
    private MetadataRegistry metadataRegistry;

    @BeforeEach
    void setUp() {
        metadataRegistry = new MetadataRegistry();
    }

    @Test
    @DisplayName("엔티티를 등록할 수 있다.")
    void register() {
        // given
        AnnotationProcessor processor = new AnnotationProcessor();
        EntityMetadata metadata = processor.processEntity(User.class);

        // when
        metadataRegistry.register(User.class, metadata);

        // then
        assertThat(metadataRegistry.getMetadata(User.class).getEntityName()).isEqualTo("User");
    }

    @Test
    @DisplayName("entity scan 시 cache에 존재하지 않으면 저장한다.")
    void scanAndRegister() {
        // given
        Class<?> entityClass = User.class;

        // when
        metadataRegistry.scanAndRegister(entityClass);

        // then
        assertThat(metadataRegistry.hasMetadata(entityClass)).isTrue();
    }

    @Test
    @DisplayName("메타데이터가 저장소에 있는지 확인할 수 있다.")
    void hasMetadata() {
        // given
        metadataRegistry.scanAndRegister(User.class);

        // when
        boolean hasMetadata = metadataRegistry.hasMetadata(User.class);

        // then
        assertThat(hasMetadata).isTrue();
    }

    @Test
    @DisplayName("메타데이터가 저장소에 존재하면 메타데이터를 가져온다.")
    void getMetadata() {
        // given
        metadataRegistry.scanAndRegister(User.class);

        // when
        EntityMetadata metadata = metadataRegistry.getMetadata(User.class);

        // then
        assertThat(metadata.getEntityName()).isEqualTo("User");
        assertThat(metadata.getIdentifierMetadata().getFieldName()).isEqualTo("id");
        assertThat(metadata.getAttributeMetadata("name").getFieldName()).isEqualTo("name");
        assertThat(metadata.getAttributeMetadata("email").getFieldName()).isEqualTo("email");
    }

    @Test
    @DisplayName("메타데이터가 저장소에 존재하지 않으면 예외를 던진다.")
    void getMetadataException() {
        // when // then
        assertThatThrownBy(() -> metadataRegistry.getMetadata(String.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("@Entity가 없는 클래스는 예외를 던진다")
    void scanNonEntityClass() {
        assertThatThrownBy(() ->
                metadataRegistry.scanAndRegister(String.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("같은 엔티티를 두 번 등록하면 덮어쓴다")
    void registerTwice() {
        // given
        metadataRegistry.scanAndRegister(User.class);

        // when
        metadataRegistry.scanAndRegister(User.class);

        // then
        assertThat(metadataRegistry.hasMetadata(User.class)).isTrue();
    }

    @Entity
    static class User {
        @Id
        private Long id;

        @Column(name = "name", nullable = false)
        private String name;

        @Column(name = "email", nullable = false)
        private String email;
    }


}