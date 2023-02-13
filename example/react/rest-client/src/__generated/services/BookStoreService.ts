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
    
    async findComplexStore(options: BookStoreServiceOptions['findComplexStore']): Promise<
        BookStoreDto['BookStoreService/COMPLEX_FETCHER'] | undefined
    > {
        let _uri = '/bookStore/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as BookStoreDto['BookStoreService/COMPLEX_FETCHER'] | undefined
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
    'findComplexStore': {readonly id: number},
    'findSimpleStores': {},
    'findStores': {},
    'saveBookStore': {readonly body: BookStoreInput}
}