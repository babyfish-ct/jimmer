import type { Dynamic, Executor } from '../';
import type { AuthorDto } from '../model/dto';
import type { Author } from '../model/entities';
import type { Gender } from '../model/enums';

export class AuthorService {
    
    constructor(private executor: Executor) {}
    
    async deleteAuthor(options: AuthorServiceOptions['deleteAuthor']): Promise<void> {
        let uri = '/author/';
        uri += encodeURIComponent(options.id);
        return (await this.executor({uri, method: 'DELETE'})) as void
    }
    
    async findAuthors(options: AuthorServiceOptions['findAuthors']): Promise<
        ReadonlyArray<AuthorDto['AuthorService/DEFAULT_FETCHER']>
    > {
        let uri = '/author/list';
        uri += '?sortCode=';
        uri += encodeURIComponent(options.sortCode);
        if (options.firstName !== undefined && options.firstName !== null) {
            uri += '&firstName=';
            uri += encodeURIComponent(options.firstName);
        }
        if (options.lastName !== undefined && options.lastName !== null) {
            uri += '&lastName=';
            uri += encodeURIComponent(options.lastName);
        }
        if (options.gender !== undefined && options.gender !== null) {
            uri += '&gender=';
            uri += encodeURIComponent(options.gender);
        }
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<AuthorDto['AuthorService/DEFAULT_FETCHER']>
    }
    
    async findComplexAuthor(options: AuthorServiceOptions['findComplexAuthor']): Promise<
        AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined
    > {
        let uri = '/author/';
        uri += encodeURIComponent(options.id);
        return (await this.executor({uri, method: 'GET'})) as AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined
    }
    
    async findSimpleAuthors(): Promise<
        ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>
    > {
        let uri = '/author/simpleList';
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>
    }
    
    async saveAuthor(): Promise<
        Dynamic<Author>
    > {
        let uri = '/author/';
        return (await this.executor({uri, method: 'PUT'})) as Dynamic<Author>
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
    'saveAuthor': {}
}