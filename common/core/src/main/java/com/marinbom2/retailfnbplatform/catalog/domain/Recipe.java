package com.marinbom2.retailfnbplatform.catalog.domain;

import com.marinbom2.retailfnbplatform.catalog.domain.exception.InvalidCatalogException;
import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외식 메뉴의 레시피. 메뉴가 팔리면 이 양만큼 식자재 재고가 줄어든다 (RecipeStockDeduction).
 * 메뉴 SKU 하나에 레시피 하나. 바꿀 때는 통째로 바꾼다 (줄 단위 수정 없음).
 */
@Getter
@Entity
@Table(name = "recipe", uniqueConstraints = @UniqueConstraint(name = "uk_recipe_menu_sku", columnNames = "menu_sku_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_sku_id", nullable = false, updatable = false)
    private Long menuSkuId;

    @ElementCollection
    @CollectionTable(name = "recipe_line", joinColumns = @JoinColumn(name = "recipe_id"))
    private List<RecipeLine> lines = new ArrayList<>();

    public static Recipe define(Sku menu, List<RecipeLine> lines) {
        if (menu.getKind() != ProductKind.MENU) {
            throw new InvalidCatalogException("레시피는 메뉴 SKU 에만 둡니다: " + menu.getSkuCode());
        }
        Recipe recipe = new Recipe();
        recipe.menuSkuId = Objects.requireNonNull(menu.getId(), "저장된 메뉴여야 합니다");
        recipe.replaceLines(lines);
        return recipe;
    }

    /** 식자재가 하나 이상이고 같은 식자재가 두 번 나오지 않아야 한다 */
    public void replaceLines(List<RecipeLine> lines) {
        if (lines.isEmpty()) {
            throw new InvalidCatalogException("레시피에는 식자재가 하나 이상 필요합니다");
        }
        if (new HashSet<>(lines.stream().map(RecipeLine::ingredientSkuId).toList()).size() != lines.size()) {
            throw new InvalidCatalogException("같은 식자재가 레시피에 두 번 들어 있습니다");
        }
        this.lines.clear();
        this.lines.addAll(lines);
    }

    public List<RecipeLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
