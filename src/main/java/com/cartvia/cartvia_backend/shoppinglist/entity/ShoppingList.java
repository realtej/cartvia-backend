package com.cartvia.cartvia_backend.shoppinglist.entity;

import com.cartvia.cartvia_backend.common.entity.BaseEntity;
import com.cartvia.cartvia_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "shopping_lists",
        indexes = @Index(name = "idx_shopping_lists_user_id", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class ShoppingList extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String name = "Weekly Groceries";

    @OneToMany(mappedBy = "shoppingList", fetch = FetchType.LAZY)
    private List<ShoppingListItem> items = new ArrayList<>();
}
