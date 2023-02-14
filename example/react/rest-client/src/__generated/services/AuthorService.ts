import type { Dynamic, Executor } from '../';
import type { AuthorDto } from '../model/dto';
import type { Author } from '../model/entities';
import type { Gender } from '../model/enums';
import type { AuthorInput } from '../model/static';

export class AuthorService {
    
    constructor(private executor: Executor) {}
    
    async deleteAuthor(options: AuthorServiceOptions['deleteAuthor']): Promise<void> {
        let _uri = '/author/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as void
    }
    
    async findAuthors(options: AuthorServiceOptions['findAuthors']): Promise<
        ReadonlyArray<AuthorDto['AuthorService/DEFAULT_FETCHER']>
    > {
        let _uri = '/author/list';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.sortCode;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += '=sortCode'
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.firstName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += '=firstName'
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.lastName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += '=lastName'
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<AuthorDto['AuthorService/DEFAULT_FETCHER']>
    }
    
    async findComplexAuthor(options: AuthorServiceOptions['findComplexAuthor']): Promise<
        AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined
    > {
        let _uri = '/author/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined
    }
    
    async findSimpleAuthors(): Promise<
        ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>
    > {
        let _uri = '/author/simpleList';
        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>
    }
    
    async saveAuthor(options: AuthorServiceOptions['saveAuthor']): Promise<
        Dynamic<Author>
    > {
        let _uri = '/author/';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.input.firstName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += '=firstName'
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.input.id;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += '=id'
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.input.lastName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += '=lastName'
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'PUT'})) as Dynamic<Author>
    }
}

export type AuthorServiceOptions = {
    'deleteAuthor': {readonly id: number},
    'findAuthors': {
        readonly sortCode: string, 
        readonly firstName?: string, 
        readonly lastName?: string, 
        readonly gender?: Gender
    },
    'findComplexAuthor': {readonly id: number},
    'findSimpleAuthors': {},
    'saveAuthor': {readonly input: AuthorInput}
}