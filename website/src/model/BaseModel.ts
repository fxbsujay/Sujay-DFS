
/**
 * 响应体对象
 * @author fxbsujay@gmail.com
 */
export interface RootObject<T>{
   code: number
   msg: string
   data: T
}
