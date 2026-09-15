package com.picturebook.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picturebook.book.domain.Book;
import com.picturebook.book.domain.BookPage;
import com.picturebook.book.domain.InteractionPoint;
import com.picturebook.book.mapper.BookMapper;
import com.picturebook.book.mapper.BookPageMapper;
import com.picturebook.book.mapper.InteractionPointMapper;
import com.picturebook.common.constant.CommonConstants;
import com.picturebook.common.core.PageResult;
import com.picturebook.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 绘本内容服务
 */
@Service
@RequiredArgsConstructor
public class BookService extends ServiceImpl<BookMapper, Book> {

    private final BookPageMapper bookPageMapper;
    private final InteractionPointMapper interactionPointMapper;

    /**
     * 用户端：分页查询已上架绘本
     */
    public PageResult<Book> listBooks(Integer pageNum, Integer pageSize,
                                       String keyword, String ageGroup, String theme) {
        Page<Book> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, CommonConstants.STATUS_ENABLED)
               .eq(Book::getAuditStatus, CommonConstants.AUDIT_PASS)
               .like(StringUtils.hasText(keyword), Book::getTitle, keyword)
               .eq(StringUtils.hasText(ageGroup), Book::getAgeGroup, ageGroup)
               .eq(StringUtils.hasText(theme), Book::getTheme, theme)
               .orderByDesc(Book::getRating)
               .orderByAsc(Book::getSortOrder);
        Page<Book> result = this.page(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords(),
                Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    /**
     * 用户端：获取绘本详情
     */
    public Book getBookDetail(Long bookId) {
        Book book = this.getById(bookId);
        if (book == null || book.getStatus() == null || book.getStatus() != CommonConstants.STATUS_ENABLED) {
            throw new BusinessException("绘本不存在或已下架");
        }
        return book;
    }

    /**
     * 获取绘本页面列表
     */
    public List<BookPage> getBookPages(Long bookId) {
        return bookPageMapper.selectList(
                new LambdaQueryWrapper<BookPage>()
                        .eq(BookPage::getBookId, bookId)
                        .orderByAsc(BookPage::getPageNum));
    }

    /**
     * 获取页面的互动点列表
     */
    public List<InteractionPoint> getPageInteractionPoints(Long pageId) {
        if (pageId == null) {
            return java.util.Collections.emptyList();
        }
        return interactionPointMapper.selectList(
                new LambdaQueryWrapper<InteractionPoint>()
                        .eq(InteractionPoint::getPageId, pageId)
                        .orderByAsc(InteractionPoint::getSortOrder));
    }

    /**
     * 根据绘本ID和页码获取互动点列表
     */
    public List<InteractionPoint> getPageInteractionPoints(Long bookId, Integer pageNum) {
        // 先通过绘本ID和页码找到页面
        BookPage page = bookPageMapper.selectOne(
                new LambdaQueryWrapper<BookPage>()
                        .eq(BookPage::getBookId, bookId)
                        .eq(BookPage::getPageNum, pageNum)
                        .last("LIMIT 1"));
        if (page == null) {
            return java.util.Collections.emptyList();
        }
        return getPageInteractionPoints(page.getPageId());
    }

    /**
     * 获取绘本的互动点列表
     */
    public List<InteractionPoint> getBookInteractionPoints(Long bookId) {
        return interactionPointMapper.selectList(
                new LambdaQueryWrapper<InteractionPoint>()
                        .eq(InteractionPoint::getBookId, bookId)
                        .orderByAsc(InteractionPoint::getSortOrder));
    }

    // ============== 管理端方法 ==============

    /**
     * 管理端：分页查询所有绘本
     */
    public PageResult<Book> adminList(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        Page<Book> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), Book::getTitle, keyword)
               .eq(status != null, Book::getStatus, status)
               .orderByDesc(Book::getCreateTime);
        Page<Book> result = this.page(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords(),
                Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
    }

    /**
     * 管理端：新增绘本
     */
    public void addBook(Book book) {
        book.setStatus(CommonConstants.STATUS_ENABLED);
        book.setAuditStatus(CommonConstants.AUDIT_PASS);
        this.save(book);
    }

    /**
     * 管理端：更新绘本
     */
    public void updateBook(Book book) {
        if (book.getBookId() == null) {
            throw new BusinessException("绘本ID不能为空");
        }
        this.updateById(book);
    }

    /**
     * 管理端：上架/下架
     */
    public void changeStatus(Long bookId, Integer status) {
        Book book = new Book();
        book.setBookId(bookId);
        book.setStatus(status);
        this.updateById(book);
    }

    /**
     * 管理端：保存绘本页面
     */
    public void savePage(BookPage page) {
        if (page.getPageId() != null) {
            bookPageMapper.updateById(page);
        } else {
            bookPageMapper.insert(page);
        }
    }

    /**
     * 管理端：删除绘本页面
     */
    public void deletePage(Long pageId) {
        bookPageMapper.deleteById(pageId);
        interactionPointMapper.delete(
                new LambdaQueryWrapper<InteractionPoint>()
                        .eq(InteractionPoint::getPageId, pageId));
    }

    /**
     * 管理端：保存互动点
     */
    public void saveInteractionPoint(InteractionPoint point) {
        if (point.getPointId() != null) {
            interactionPointMapper.updateById(point);
        } else {
            interactionPointMapper.insert(point);
        }
    }

    /**
     * 管理端：删除互动点
     */
    public void deleteInteractionPoint(Long pointId) {
        interactionPointMapper.deleteById(pointId);
    }
}
