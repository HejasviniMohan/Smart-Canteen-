package com.canteen.service;

import com.canteen.dto.CategoryDtos;
import com.canteen.entity.Category;
import com.canteen.exception.ApiException;
import com.canteen.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repo;

    @InjectMocks
    private CategoryService service;

    private Category southIndian;

    @BeforeEach
    void setUp() {
        southIndian = Category.builder()
                .id(1L).name("South Indian").description("Dosa, Idli")
                .displayOrder(1).build();
    }

    @Test
    @DisplayName("findAll: returns mapped DTOs for all categories")
    void findAll_returnsDtos() {
        when(repo.findAll()).thenReturn(List.of(southIndian));

        List<CategoryDtos.Response> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("South Indian");
        assertThat(result.get(0).getDisplayOrder()).isEqualTo(1);
        verify(repo, times(1)).findAll();
    }

    @Test
    @DisplayName("findById: returns mapped DTO when category exists")
    void findById_returnsWhenExists() {
        when(repo.findById(1L)).thenReturn(Optional.of(southIndian));

        CategoryDtos.Response result = service.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("South Indian");
    }

    @Test
    @DisplayName("findById: throws 404 ApiException when missing")
    void findById_throwsWhenMissing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create: persists new category when name is unique")
    void create_persistsWhenUnique() {
        CategoryDtos.CreateRequest req = CategoryDtos.CreateRequest.builder()
                .name("Beverages").description("Drinks").displayOrder(4).build();

        when(repo.existsByName("Beverages")).thenReturn(false);
        when(repo.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(10L);
                return c;
            });

        CategoryDtos.Response result = service.create(req);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Beverages");
        assertThat(result.getDisplayOrder()).isEqualTo(4);
        verify(repo).save(any(Category.class));
    }

    @Test
    @DisplayName("create: rejects duplicate name with 409")
    void create_rejectsDuplicateName() {
        CategoryDtos.CreateRequest req = CategoryDtos.CreateRequest.builder()
                .name("South Indian").build();

        when(repo.existsByName("South Indian")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("update: applies partial changes and persists")
    void update_appliesPartialChanges() {
        CategoryDtos.UpdateRequest req = CategoryDtos.UpdateRequest.builder()
                .description("Updated desc").displayOrder(5).build();

        when(repo.findById(1L)).thenReturn(Optional.of(southIndian));
        when(repo.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDtos.Response result = service.update(1L, req);

        assertThat(result.getDescription()).isEqualTo("Updated desc");
        assertThat(result.getDisplayOrder()).isEqualTo(5);
        assertThat(result.getName()).isEqualTo("South Indian");
    }

    @Test
    @DisplayName("update: rejects name change that conflicts with another category")
    void update_rejectsConflictingName() {
        CategoryDtos.UpdateRequest req = CategoryDtos.UpdateRequest.builder().name("North Indian").build();

        when(repo.findById(1L)).thenReturn(Optional.of(southIndian));
        when(repo.existsByName("North Indian")).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    @DisplayName("update: allows keeping the same name on the same record")
    void update_allowsSameName() {
        CategoryDtos.UpdateRequest req = CategoryDtos.UpdateRequest.builder()
                .name("South Indian").description("Same name, new desc").build();

        when(repo.findById(1L)).thenReturn(Optional.of(southIndian));
        when(repo.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDtos.Response result = service.update(1L, req);

        assertThat(result.getDescription()).isEqualTo("Same name, new desc");
    }

    @Test
    @DisplayName("delete: removes category when it exists")
    void delete_removesWhenExists() {
        when(repo.findById(1L)).thenReturn(Optional.of(southIndian));

        service.delete(1L);

        verify(repo).delete(southIndian);
    }

    @Test
    @DisplayName("delete: throws 404 when category is missing")
    void delete_throwsWhenMissing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ApiException.class);
        verify(repo, never()).delete(any());
    }
}