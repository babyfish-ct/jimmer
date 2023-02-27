import type { Dynamic, Executor } from '../';
import type { BookStoreDto } from '../model/dto';
import type { BookStore } from '../model/entities';
import type { BookStoreInput } from '../model/static';

export class BookStoreService {
    
    constructor(private executor: Executor) {}
    
    async deleteBookStore(options: BookStoreServiceOptions['deleteBookStore']): Promise<void> {
        let _uri = '/bookStore/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as void
    }
    
    async findComplexStoreWithAllBooks(options: BookStoreServiceOptions['findComplexStoreWithAllBooks']): Promise<
        BookStoreDto['BookStoreService/WITH_ALL_BOOKS_FETCHER'] | undefined
    > {
        let _uri = '/bookStore/';
        _uri += encodeURIComponent(options.id);
        _uri += '/withAllBooks';
        return (await this.executor({uri: _uri, method: 'GET'})) as BookStoreDto['BookStoreService/WITH_ALL_BOOKS_FETCHER'] | undefined
    }
    
    async findComplexStoreWithNewestBooks(options: BookStoreServiceOptions['findComplexStoreWithNewestBooks']): Promise<
        BookStoreDto['BookStoreService/WITH_NEWEST_BOOKS_FETCHER'] | undefined
    > {
        let _uri = '/bookStore/';
        _uri += encodeURIComponent(options.id);
        _uri += '/withNewestBooks';
        return (await this.executor({uri: _uri, method: 'GET'})) as BookStoreDto['BookStoreService/WITH_NEWEST_BOOKS_FETCHER'] | undefined
    }
    
    async findSimpleStores(): Promise<
        ReadonlyArray<BookStoreDto['BookStoreService/SIMPLE_FETCHER']>
    > {
        let _uri = '/bookStore/simpleList';
        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<BookStoreDto['BookStoreService/SIMPLE_FETCHER']>
    }
    
    async findStores(): Promise<
        ReadonlyArray<BookStoreDto['BookStoreService/DEFAULT_FETCHER']>
    > {
        let _uri = '/bookStore/list';
        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<BookStoreDto['BookStoreService/DEFAULT_FETCHER']>
    }
    
    async saveBookStore(options: BookStoreServiceOptions['saveBookStore']): Promise<
        Dynamic<BookStore>
    > {
        let _uri = '/bookStore/';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Dynamic<BookStore>
    }
}

export type BookStoreServiceOptions = {
    'deleteBookStore': {readonly id: number},
    'findComplexStoreWithAllBooks': {readonly id: number},
    'findComplexStoreWithNewestBooks': {readonly id: number},
    'findSimpleStores': {},
    'findStores': {},
    'saveBookStore': {readonly body: BookStoreInput}
}