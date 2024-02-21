import type {Executor} from '../';
import type {BookStoreDto} from '../model/dto/';
import type {Dynamic_BookStore} from '../model/dynamic/';
import type {BookStoreInput} from '../model/static/';

export class BookStoreService {
    
    constructor(private executor: Executor) {}
    
    readonly deleteBookStore: (options: BookStoreServiceOptions['deleteBookStore']) => Promise<
        void
    > = async(options) => {
        let _uri = '/bookStore/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findComplexStoreWithAllBooks: (options: BookStoreServiceOptions['findComplexStoreWithAllBooks']) => Promise<
        BookStoreDto['BookStoreService/WITH_ALL_BOOKS_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/bookStore/';
        _uri += encodeURIComponent(options.id);
        _uri += '/withAllBooks';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<BookStoreDto['BookStoreService/WITH_ALL_BOOKS_FETCHER'] | undefined>;
    }
    
    readonly findComplexStoreWithNewestBooks: (options: BookStoreServiceOptions['findComplexStoreWithNewestBooks']) => Promise<
        BookStoreDto['BookStoreService/WITH_NEWEST_BOOKS_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/bookStore/';
        _uri += encodeURIComponent(options.id);
        _uri += '/withNewestBooks';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<BookStoreDto['BookStoreService/WITH_NEWEST_BOOKS_FETCHER'] | undefined>;
    }
    
    readonly findComplexStores: () => Promise<
        ReadonlyArray<BookStoreDto['BookStoreService/WITH_ALL_BOOKS_FETCHER']>
    > = async() => {
        let _uri = '/bookStore/complexList';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<BookStoreDto['BookStoreService/WITH_ALL_BOOKS_FETCHER']>>;
    }
    
    readonly findSimpleStores: () => Promise<
        ReadonlyArray<BookStoreDto['BookStoreService/SIMPLE_FETCHER']>
    > = async() => {
        let _uri = '/bookStore/simpleList';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<BookStoreDto['BookStoreService/SIMPLE_FETCHER']>>;
    }
    
    readonly findStores: () => Promise<
        ReadonlyArray<BookStoreDto['BookStoreService/DEFAULT_FETCHER']>
    > = async() => {
        let _uri = '/bookStore/list';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<BookStoreDto['BookStoreService/DEFAULT_FETCHER']>>;
    }
    
    readonly saveBookStore: (options: BookStoreServiceOptions['saveBookStore']) => Promise<
        Dynamic_BookStore
    > = async(options) => {
        let _uri = '/bookStore/';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<Dynamic_BookStore>;
    }
}

export type BookStoreServiceOptions = {
    'findSimpleStores': {}, 
    'findStores': {}, 
    'findComplexStores': {}, 
    'findComplexStoreWithAllBooks': {
        readonly id: number
    }, 
    'findComplexStoreWithNewestBooks': {
        readonly id: number
    }, 
    'saveBookStore': {
        readonly body: BookStoreInput
    }, 
    'deleteBookStore': {
        readonly id: number
    }
}
