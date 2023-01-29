import type { BookStore } from '../model/entities';
import type { Unit, Tuple2, BookStoreInput } from '../model/static';
import type { BookStoreDto, BookDto } from '../model/dto';
import type { Executor, Dynamic } from '../';

export class BookStoreService {
    
    constructor(private executor: Executor) {}
    
    async deleteBookStore(options: BookStoreServiceOptions['deleteBookStore']): Promise<
        Unit
    > {
        let uri = '/bookStore/';
        uri += encodeURIComponent(options.id);
        return (await this.executor({uri, method: 'DELETE'})) as Unit
    }
    
    async findComplexStores(): Promise<
        ReadonlyArray<BookStoreDto['BookStoreService/COMPLEX_FETCHER']>
    > {
        let uri = '/bookStore/complexList';
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<BookStoreDto['BookStoreService/COMPLEX_FETCHER']>
    }
    
    async findSimpleStores(): Promise<
        ReadonlyArray<BookStoreDto['BookStoreService/SIMPLE_FETCHER']>
    > {
        let uri = '/bookStore/simpleList';
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<BookStoreDto['BookStoreService/SIMPLE_FETCHER']>
    }
    
    async findStores(): Promise<
        ReadonlyArray<BookStoreDto['BookStoreService/DEFAULT_FETCHER']>
    > {
        let uri = '/bookStore/list';
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<BookStoreDto['BookStoreService/DEFAULT_FETCHER']>
    }
    
    async findStoresWithNewestBook(): Promise<
        ReadonlyArray<Tuple2<BookStoreDto['BookStoreService/SIMPLE_FETCHER'], BookDto['BookStoreService/NEWEST_BOOK_FETCHER'] | undefined>>
    > {
        let uri = '/bookStore/withNewestBook';
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<Tuple2<BookStoreDto['BookStoreService/SIMPLE_FETCHER'], BookDto['BookStoreService/NEWEST_BOOK_FETCHER'] | undefined>>
    }
    
    async saveBookStore(options: BookStoreServiceOptions['saveBookStore']): Promise<
        Dynamic<BookStore>
    > {
        let uri = '/bookStore/';
        return (await this.executor({uri, method: 'PUT', body: options.body})) as Dynamic<BookStore>
    }
}

export type BookStoreServiceOptions = {
    'deleteBookStore': {readonly id: number},
    'findComplexStores': {},
    'findSimpleStores': {},
    'findStores': {},
    'findStoresWithNewestBook': {},
    'saveBookStore': {readonly body: BookStoreInput}
}