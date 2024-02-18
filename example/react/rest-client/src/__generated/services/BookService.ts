import type {Executor} from '../';
import type {BookDto} from '../model/dto/';
import type {Dynamic_Book} from '../model/dynamic/';
import type {
    BookInput, 
    BookSpecification, 
    CompositeBookInput, 
    Page
} from '../model/static/';

export class BookService {
    
    constructor(private executor: Executor) {}
    
    readonly deleteBook: (options: BookServiceOptions['deleteBook']) => Promise<
        void
    > = async(options) => {
        let _uri = '/book/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    /**
     * The functionality of this method is the same as
     * {@link #findBooksBySuperQBE(int, int, String, BookSpecification)}
     */
    readonly findBooks: (options: BookServiceOptions['findBooks']) => Promise<
        Page<BookDto['BookService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/book/list';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.pageIndex;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageIndex='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.pageSize;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageSize='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.sortCode;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'sortCode='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.name;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'name='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.minPrice;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'minPrice='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.maxPrice;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'maxPrice='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.storeName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'storeName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.authorName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'authorName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<BookDto['BookService/DEFAULT_FETCHER']>>;
    }
    
    /**
     * The functionality of this method is the same as
     * {@link #findBooks(int, int, String, String, BigDecimal, BigDecimal, String, String)}
     */
    readonly findBooksBySuperQBE: (options: BookServiceOptions['findBooksBySuperQBE']) => Promise<
        Page<BookDto['BookService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/book/list/bySuperQBE';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.specification.name;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'name='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.minPrice;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'minPrice='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.maxPrice;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'maxPrice='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.storeName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'storeName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.authorName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'authorName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.pageIndex;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageIndex='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.pageSize;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageSize='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.sortCode;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'sortCode='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<BookDto['BookService/DEFAULT_FETCHER']>>;
    }
    
    readonly findComplexBook: (options: BookServiceOptions['findComplexBook']) => Promise<
        BookDto['BookService/COMPLEX_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/book/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<BookDto['BookService/COMPLEX_FETCHER'] | undefined>;
    }
    
    readonly findSimpleBooks: () => Promise<
        ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>
    > = async() => {
        let _uri = '/book/simpleList';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>>;
    }
    
    readonly saveBook: (options: BookServiceOptions['saveBook']) => Promise<
        Dynamic_Book
    > = async(options) => {
        let _uri = '/book/';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<Dynamic_Book>;
    }
    
    readonly saveCompositeBook: (options: BookServiceOptions['saveCompositeBook']) => Promise<
        Dynamic_Book
    > = async(options) => {
        let _uri = '/book/composite';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<Dynamic_Book>;
    }
}

export type BookServiceOptions = {
    'findSimpleBooks': {}, 
    'findBooks': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly name?: string | undefined, 
        readonly minPrice?: number | undefined, 
        readonly maxPrice?: number | undefined, 
        readonly storeName?: string | undefined, 
        readonly authorName?: string | undefined
    }, 
    'findBooksBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: BookSpecification
    }, 
    'findComplexBook': {
        readonly id: number
    }, 
    'saveBook': {
        readonly body: BookInput
    }, 
    'saveCompositeBook': {
        readonly body: CompositeBookInput
    }, 
    'deleteBook': {
        readonly id: number
    }
}
