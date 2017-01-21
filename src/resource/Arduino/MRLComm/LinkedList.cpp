#ifndef LinkedList_cpp
#define LinkedList_cpp

#include "LinkedList.h"

// Initialize LinkedList with false values
template<typename T>
LinkedList<T>::LinkedList() {
  root=NULL;
  last=NULL;
  _size=0;
  lastNodeGot = root;
  lastIndexGot = 0;
  isCached = false;
}

// Clear Nodes and free Memory
template<typename T>
LinkedList<T>::~LinkedList() {
  ListNode<T>* tmp;
  while(root!=false) {
    tmp=root;
    root=root->next;
    delete tmp;
  }
  last = NULL;
  _size=0;
  isCached = false;
}

/* Actualy "logic" coding */
template<typename T>
ListNode<T>* LinkedList<T>::getNode(int index) {
  int _pos = 0;
  ListNode<T>* current = root;
  // Check if the node trying to get is
  // immediatly AFTER the previous got one
  if(isCached && lastIndexGot <= index) {
    _pos = lastIndexGot;
    current = lastNodeGot;
  }
  while(_pos < index && current) {
    current = current->next;
    _pos++;
  }
  // Check if the object index got is the same as the required
  if(_pos == index) {
    isCached = true;
    lastIndexGot = index;
    lastNodeGot = current;
    return current;
  }
  return false;
}

template<typename T>
int LinkedList<T>::size() {
  return _size;
}

template<typename T>
bool LinkedList<T>::add(int index, T _t) {
  if(index >= _size)
    return add(_t);
  if(index == 0)
    return unshift(_t);
  ListNode<T> *tmp = new ListNode<T>(),
    *_prev = getNode(index-1);
  tmp->data = _t;
  tmp->next = _prev->next;
  _prev->next = tmp;
  _size++;
  isCached = false;
  return true;
}

template<typename T>
bool LinkedList<T>::add(T _t) {
  ListNode<T> *tmp = new ListNode<T>();
  tmp->data = _t;
  tmp->next = NULL;
  if(root) {
    // Already have elements inserted
    last->next = tmp;
    last = tmp;
  } else {
    // First element being inserted
    root = tmp;
    last = tmp;
  }
  _size++;
  isCached = false;
  return true;
}

template<typename T>
bool LinkedList<T>::unshift(T _t) {
  if(_size == 0)
    return add(_t);
  ListNode<T> *tmp = new ListNode<T>();
  tmp->next = root;
  tmp->data = _t;
  root = tmp;
  _size++;
  isCached = false;
  return true;
}

template<typename T>
bool LinkedList<T>::set(int index, T _t) {
  // Check if index position is in bounds
  if(index < 0 || index >= _size)
    return false;
  getNode(index)->data = _t;
  return true;
}

template<typename T>
T LinkedList<T>::pop() {
  if(_size <= 0)
    return T();
  isCached = false;
  if(_size >= 2) {
    ListNode<T> *tmp = getNode(_size - 2);
    T ret = tmp->next->data;
    delete(tmp->next);
    tmp->next = NULL;
    last = tmp;
    _size--;
    return ret;
  } else {
    // Only one element left on the list
    T ret = root->data;
    delete(root);
    root = NULL;
    last = NULL;
    _size = 0;
    return ret;
  }
}

template<typename T>
T LinkedList<T>::shift() {
  if (_size <= 0)
    return T();
  if (_size > 1) {
    ListNode<T> *_next = root->next;
    T ret = root->data;
    delete(root);
    root = _next;
    _size --;
    isCached = false;
    return ret;
  } else {
    // Only one left, then pop()
    return pop();
  }
}

template<typename T>
T LinkedList<T>::remove(int index) {
  if (index < 0 || index >= _size) {
    return T();
  }
  if(index == 0)
    return shift();
  if (index == _size-1) {
    return pop();
  }
  ListNode<T> *tmp = getNode(index - 1);
  ListNode<T> *toDelete = tmp->next;
  T ret = toDelete->data;
  tmp->next = tmp->next->next;
  delete(toDelete);
  _size--;
  isCached = false;
  return ret;
}

template<typename T>
T LinkedList<T>::get(int index){
  ListNode<T> *tmp = getNode(index);
  return (tmp ? tmp->data : T());
}

template<typename T>
void LinkedList<T>::clear() {
  while(size() > 0)
    shift();
}

template<typename T>
ListNode<T>* LinkedList<T>::getRoot() {
  return root;
}

#endif
