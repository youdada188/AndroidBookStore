package com.yzc.bookmall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Address;
import com.yzc.bookmall.model.Book;
import com.yzc.bookmall.model.CartItem;
import com.yzc.bookmall.model.Coupon;
import com.yzc.bookmall.model.ReceiverInfo;
import com.yzc.bookmall.model.Store;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class BookDatabaseTest {
    private BookRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        repository = new BookRepository(context);
    }

    private String uniqueName(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    @Test
    public void firstOpenSeedsStoresCategoriesAndBooks() {
        assertEquals(5, repository.getStores().size());
        assertEquals(5, repository.getCategories().size());
        assertEquals(20, repository.getAllBooks().size());
        assertTrue(repository.getRecommendedBooks().size() >= 1);
        assertNotNull(repository.getBookById(1L));
    }

    @Test
    public void everyBookBelongsToAnExistingStore() {
        for (Book book : repository.getAllBooks()) {
            Store store = repository.getStoreById(book.getStoreId());
            assertNotNull("图书《" + book.getTitle() + "》没有关联书店", store);
        }
    }

    @Test
    public void storeDetailListsOnlyItsOwnBooks() {
        List<Book> books = repository.getBooksByStore(1L);
        assertFalse(books.isEmpty());
        for (Book book : books) assertEquals(1L, book.getStoreId());
    }

    @Test
    public void addingSameBookTwiceMergesCartQuantity() {
        long userId = repository.registerUser(uniqueName("cart"), "123456");
        Book book = repository.getBookById(1L);

        repository.addToCart(userId, book.getId(), 1);
        repository.addToCart(userId, book.getId(), 2);

        List<CartItem> items = repository.getCartItems(userId);
        assertEquals(1, items.size());
        assertEquals(3, items.get(0).getQuantity());
    }

    @Test
    public void insufficientStockRollsBackOrderAndStock() {
        long userId = repository.registerUser(uniqueName("rollback"), "123456");
        Book book = repository.getBookById(1L);
        int originalStock = book.getStock();
        CartItem invalidItem = new CartItem(1L, userId, book.getId(), book.getTitle(), book.getAuthor(),
                book.getPriceFen(), book.getCoverResId(), originalStock + 1);

        boolean failed = false;
        try {
            repository.createOrder(userId, Arrays.asList(invalidItem),
                    new ReceiverInfo("张三", "13800000000", "北京市海淀区"), 0, 0, null, 0);
        } catch (IllegalStateException expected) {
            failed = true;
        }

        assertTrue(failed);
        assertEquals(originalStock, repository.getBookById(book.getId()).getStock());
        assertTrue(repository.getOrders(userId).isEmpty());
    }

    @Test
    public void successfulOrderDecrementsStockAndClearsCart() {
        long userId = repository.registerUser(uniqueName("order"), "123456");
        Book book = repository.getBookById(2L);
        int originalStock = book.getStock();

        repository.addToCart(userId, book.getId(), 1);
        List<CartItem> items = repository.getCartItems(userId);
        long orderId = repository.createOrder(userId, items,
                new ReceiverInfo("李四", "13900000000", "上海市松江区"), 500, 0, null, 0);

        assertTrue(orderId > 0);
        assertEquals(originalStock - 1, repository.getBookById(book.getId()).getStock());
        assertTrue(repository.getCartItems(userId).isEmpty());
        assertEquals(1, repository.getOrders(userId).size());
        assertEquals(1, repository.getOrderItems(orderId).size());
    }

    @Test
    public void addressCanBeSavedAndSetAsDefault() {
        long userId = repository.registerUser(uniqueName("addr"), "123456");
        long first = repository.saveAddress(userId, 0, "王五", "13700000000", "浙江省杭州市", "西湖区 1 号", false);
        long second = repository.saveAddress(userId, 0, "赵六", "13600000000", "江苏省南京市", "鼓楼区 2 号", true);

        assertTrue(first > 0 && second > 0);
        Address defaultAddress = repository.getDefaultAddress(userId);
        assertNotNull(defaultAddress);
        assertEquals("赵六", defaultAddress.getName());

        repository.setDefaultAddress(userId, first);
        assertEquals("王五", repository.getDefaultAddress(userId).getName());

        repository.deleteAddress(userId, second);
        assertEquals(1, repository.getAddresses(userId).size());
    }

    @Test
    public void reviewRequiresPurchase() {
        long userId = repository.registerUser(uniqueName("review"), "123456");
        Book book = repository.getBookById(3L);
        int before = repository.getReviewCount(book.getId());

        assertFalse(repository.canReview(userId, book.getId()));

        repository.addToCart(userId, book.getId(), 1);
        repository.createOrder(userId, repository.getCartItems(userId),
                new ReceiverInfo("孙七", "13500000000", "广东省广州市"), 0, 0, null, 0);

        assertTrue(repository.canReview(userId, book.getId()));
        repository.addReview(userId, book.getId(), 5, "很好的一本书");
        assertFalse(repository.canReview(userId, book.getId()));
        assertEquals(before + 1, repository.getReviewCount(book.getId()));
    }

    @Test
    public void couponMatchesThreshold() {
        long userId = repository.registerUser(uniqueName("coupon"), "123456");
        repository.claimCoupons(userId);

        assertTrue(repository.findBestCoupon(userId, 1000) == null);

        Coupon best = repository.findBestCoupon(userId, 35000);
        assertNotNull(best);
        assertTrue(best.getThresholdFen() <= 35000);
        assertEquals(6000, best.getDiscountFen());
    }

    @Test
    public void clearingOneUserKeepsCatalogAndOtherUserData() {
        long firstUser = repository.registerUser(uniqueName("first"), "123456");
        long secondUser = repository.registerUser(uniqueName("second"), "123456");
        Book book = repository.getBookById(1L);

        repository.setFavorite(firstUser, book.getId(), true);
        repository.addToCart(firstUser, book.getId(), 1);
        repository.setFavorite(secondUser, book.getId(), true);
        repository.saveAddress(firstUser, 0, "周八", "13400000000", "四川省成都市", "武侯区 3 号", true);

        repository.clearCurrentUserData(firstUser);

        assertTrue(repository.getFavoriteBooks(firstUser).isEmpty());
        assertTrue(repository.getCartItems(firstUser).isEmpty());
        assertTrue(repository.getAddresses(firstUser).isEmpty());
        assertFalse(repository.getFavoriteBooks(secondUser).isEmpty());
        assertNotNull(repository.getBookById(book.getId()));
    }
}
