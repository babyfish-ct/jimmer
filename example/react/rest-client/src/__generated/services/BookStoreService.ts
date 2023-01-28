import type { Tuple2 } from '../model/static';
import type { BookStoreDto, BookDto } from '../model/dto';
import type { Executor } from '../';

export class BookStoreService {
    
    constructor(private executor: Executor) {}
    
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
        let uri = '/bookStore/listWithNewestBook';
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<Tuple2<BookStoreDto['BookStoreService/SIMPLE_FETCHER'], BookDto['BookStoreService/NEWEST_BOOK_FETCHER'] | undefined>>
    }
}

export type BookStoreServiceOptions = {
    'findComplexStores': {},
    'findSimpleStores': {},
    'findStores': {},
    'findStoresWithNewestBook': {}
}