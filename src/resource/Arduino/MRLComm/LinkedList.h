/*
  LinkedList.h - V1.1 - Generic LinkedList implementation
  Works better with FIFO, because LIFO will need to
  search the entire List to find the last one;
  For instructions, go to https://github.com/ivanseidel/LinkedList
  Created by Ivan Seidel Gomes, March, 2013.
  Released into the public domain.
*/

#ifndef LinkedList_h
#define LinkedList_h

#include <Arduino.h>

template<class T>
struct ListNode
{
  T data;
  ListNode<T> *next;
};

template <typename T>
class LinkedList{

protected:
  int _size;
  ListNode<T> *root;
  ListNode<T> *last;
  // Helps "get" method, by saving last position
  ListNode<T> *lastNodeGot;
  int lastIndexGot;
  // isCached should be set to FALSE
  // everytime the list suffer changes
  bool isCached;
  ListNode<T>* getNode(int id);

public:
  LinkedList();
  ~LinkedList();
  /* Returns current size of LinkedList */
  virtual int size();
  /* Adds a T object in the specified index;
    Unlink and link the LinkedList correcly;
    Increment _size */
  virtual bool add(int index, T);
  /* Adds a T object in the end of the LinkedList;
    Increment _size; */
  virtual bool add(T);
  /* Adds a T object in the start of the LinkedList;
    Increment _size; */
  virtual bool unshift(T);
  /* Set the object at index, with T;
    Increment _size; */
  virtual bool set(int index, T);
  /* Remove object at index;
    If index is not reachable, returns false;
    else, decrement _size */
  virtual T remove(int index);
  /* Remove last object; */
  virtual T pop();
  /* Remove first object; */
  virtual T shift();
  /* Get the index'th element on the list;
    Return Element if accessible,
    else, return false; */
  virtual T get(int index);
  /* Clear the entire array */
  virtual void clear();
  virtual ListNode<T>* getRoot();
};

#include "LinkedList.cpp"

#endif


